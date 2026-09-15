import { provideRouter, Router } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { AuthService } from '@core/authentication';
import { UserPanel } from './user-panel';

describe('UserPanel', () => {
  let fixture: ComponentFixture<UserPanel>;
  let component: UserPanel;
  let router: Router;
  const auth = {
    user: () =>
      of({
        name: 'Admin Demo',
        email: 'admin@example.test',
        avatar: '',
        permissions: ['MANAGE_USERS'],
      }),
    logout: () => of(true),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserPanel],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    }).compileComponents();

    fixture = TestBed.createComponent(UserPanel);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => fixture?.destroy());

  it('keeps the profile menu trigger available in the collapsed sidebar', () => {
    const trigger = fixture.nativeElement.querySelector(
      'button[aria-label="Open profile menu"]'
    ) as HTMLButtonElement | null;

    expect(trigger).not.toBeNull();
  });

  it('shows the Staff accounts shortcut to an Administrator', async () => {
    const trigger = fixture.nativeElement.querySelector(
      'button[aria-label="Open profile menu"]'
    ) as HTMLButtonElement;

    trigger.click();
    await fixture.whenStable();

    const items = Array.from(document.querySelectorAll<HTMLButtonElement>('[mat-menu-item]'));
    expect(items.some(item => item.textContent?.includes('Staff accounts'))).toBe(true);
  });

  it('logs out and returns to login', () => {
    vi.spyOn(auth, 'logout');
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    component.logout();

    expect(auth.logout).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/auth/login');
  });
});
