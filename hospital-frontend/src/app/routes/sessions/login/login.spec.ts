import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AuthService } from '@core/authentication';
import { provideTranslateService } from '@ngx-translate/core';
import { Observable, Subject, of } from 'rxjs';
import { vi } from 'vitest';

import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let component: Login;
  let login: (username: string, password: string) => Observable<boolean>;
  let navigateByUrl: (url: string) => Promise<boolean>;

  beforeEach(async () => {
    login = () => of(true);
    navigateByUrl = () => Promise.resolve(true);

    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideTranslateService(),
        {
          provide: AuthService,
          useValue: {
            login: (...args: [string, string]) => login(...args),
            landingPath: () => '/patients',
          },
        },
        { provide: Router, useValue: { navigateByUrl: (url: string) => navigateByUrl(url) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  afterEach(() => fixture.destroy());

  it('renders a simple login form without icons', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('mat-icon')).toBeNull();
    expect(element.textContent).not.toContain('👋');
    expect(element.querySelectorAll('input')).toHaveLength(2);
  });

  it('submits valid credentials and opens the application', () => {
    login = vi.fn(() => of(true));
    navigateByUrl = vi.fn(() => Promise.resolve(true));
    component.loginForm.setValue({ username: 'doctor', password: 'Doctor@123' });

    component.login();

    expect(login).toHaveBeenCalledWith('doctor', 'Doctor@123');
    expect(navigateByUrl).toHaveBeenCalledWith('/patients');
    expect(component.isSubmitting()).toBe(false);
  });

  it('does not submit an invalid form', () => {
    login = vi.fn(() => of(true));

    component.login();

    expect(login).not.toHaveBeenCalled();
    expect(component.loginForm.touched).toBe(true);
  });

  function enter(field: string, value: string) {
    const input = fixture.nativeElement.querySelector(`[formControlName="${field}"]`);
    input.value = value;
    input.dispatchEvent(new Event('input', { bubbles: true }));
  }

  it.each([401, 0])('allows another attempt after an asynchronous %s failure', async status => {
    const response = new Subject<boolean>();
    login = vi.fn().mockReturnValueOnce(response).mockReturnValueOnce(of(true));
    navigateByUrl = vi.fn(() => Promise.resolve(true));
    enter('username', '001201100002');
    enter('password', 'IncorrectPassword');
    await fixture.whenStable();
    const submit = fixture.nativeElement.querySelector('button[type="submit"]');

    submit.click();
    await fixture.whenStable();
    expect(submit.disabled).toBe(true);
    response.error(new HttpErrorResponse({ status, error: { message: 'Sign-in failed' } }));
    await fixture.whenStable();

    expect(submit.disabled).toBe(false);
    expect(submit.getAttribute('aria-busy')).toBe('false');
    expect(fixture.nativeElement.textContent).toContain('Sign-in failed');
    enter('password', 'CorrectedPassword');
    await fixture.whenStable();
    submit.click();
    await fixture.whenStable();

    expect(login).toHaveBeenCalledTimes(2);
    expect(login).toHaveBeenLastCalledWith('001201100002', 'CorrectedPassword');
    expect(navigateByUrl).toHaveBeenCalledWith('/patients');
  });

  it('prevents duplicate requests while a login is pending', async () => {
    const response = new Subject<boolean>();
    login = vi.fn(() => response);
    enter('username', '001201100002');
    enter('password', 'ExamplePassword');

    component.login();
    component.login();
    await fixture.whenStable();

    expect(login).toHaveBeenCalledTimes(1);
    response.complete();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(false);
  });

  it('keeps server field validation until the affected value is corrected', async () => {
    const response = new Subject<boolean>();
    login = () => response;
    enter('username', '001201100002');
    enter('password', 'ExamplePassword');
    component.login();
    response.error(
      new HttpErrorResponse({
        status: 400,
        error: { errors: { username: 'Invalid username format' } },
      })
    );
    await fixture.whenStable();

    expect(component.username.errors?.['remote']).toBe('Invalid username format');
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
    enter('username', '001201100003');
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(false);
  });
});
