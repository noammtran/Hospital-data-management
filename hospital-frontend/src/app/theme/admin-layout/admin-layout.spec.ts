import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NavigationEnd, Router, provideRouter } from '@angular/router';
import { Menu, MenuService, SettingsService, defaults } from '@core';
import { AuthService, User } from '@core/authentication';
import { provideTranslateService } from '@ngx-translate/core';
import { NgxPermissionsModule, NgxPermissionsService } from 'ngx-permissions';
import { BehaviorSubject, Subject } from 'rxjs';
import { vi } from 'vitest';
import menuData from '../../../../public/data/menu.json';
import { AdminLayout } from './admin-layout';

function screen(mobile: boolean): BreakpointState {
  return {
    matches: true,
    breakpoints: {
      'screen and (max-width: 599px)': mobile,
      'screen and (min-width: 600px)': !mobile,
    },
  };
}

describe('AdminLayout navigation', () => {
  let fixture: ComponentFixture<AdminLayout>;
  let viewport: BehaviorSubject<BreakpointState>;
  let navigation: Subject<NavigationEnd>;
  let user: BehaviorSubject<User>;
  const settings = {
    options: { ...defaults, sidenavOpened: false },
    setOptions: vi.fn(),
    getThemeColor: () => 'light',
  };

  beforeEach(async () => {
    viewport = new BehaviorSubject(screen(false));
    navigation = new Subject();
    user = new BehaviorSubject<User>({ name: 'Fictional account', permissions: [] });
    settings.options = { ...defaults, sidenavOpened: false, sidenavCollapsed: false };
    settings.setOptions.mockClear();
    await TestBed.configureTestingModule({
      imports: [AdminLayout, NgxPermissionsModule.forRoot()],
      providers: [
        provideRouter([]),
        provideTranslateService(),
        { provide: BreakpointObserver, useValue: { observe: () => viewport } },
        { provide: SettingsService, useValue: settings },
        { provide: AuthService, useValue: { user: () => user } },
      ],
    }).compileComponents();
    vi.spyOn(TestBed.inject(Router), 'events', 'get').mockReturnValue(navigation);
    TestBed.inject(MenuService).set(structuredClone(menuData.menu) as Menu[]);
  });

  afterEach(() => fixture?.destroy());

  async function render() {
    fixture = TestBed.createComponent(AdminLayout);
    await fixture.whenStable();
    vi.spyOn(fixture.componentInstance.content(), 'scrollTo').mockImplementation(() => undefined);
    return fixture.componentInstance;
  }

  it.each([
    {
      role: 'RECEPTIONIST',
      permissions: ['CARE_VIEW', 'VIEW_PATIENT'],
      links: ['/care', '/patients'],
    },
    {
      role: 'DOCTOR',
      permissions: ['CARE_VIEW', 'VIEW_PATIENT', 'VIEW_STATISTICS'],
      links: ['/care', '/patients', '/statistics'],
    },
    { role: 'NURSE', permissions: ['CARE_VIEW', 'VIEW_PATIENT'], links: ['/care', '/patients'] },
    { role: 'TECHNICIAN', permissions: ['CARE_VIEW'], links: ['/care'] },
    {
      role: 'ADMINISTRATOR',
      permissions: [
        'CARE_VIEW',
        'VIEW_PATIENT',
        'VIEW_AUDIT_LOG',
        'VIEW_STATISTICS',
        'MANAGE_USERS',
      ],
      links: ['/care', '/patients', '/audit-log', '/statistics', '/users'],
    },
    { role: 'PATIENT', permissions: ['VIEW_OWN_RECORD'], links: ['/my-record'] },
  ])(
    'opens the sidebar with only permitted links for $role despite saved closed state',
    async ({ permissions, links }) => {
      TestBed.inject(NgxPermissionsService).loadPermissions(permissions);
      const layout = await render();

      expect(layout.sidenav().opened).toBe(true);
      await vi.waitFor(() => {
        const anchors = Array.from(
          fixture.nativeElement.querySelectorAll('app-sidemenu a')
        ) as HTMLAnchorElement[];
        expect(anchors.map(anchor => anchor.getAttribute('href'))).toEqual(links);
      });
      expect(
        fixture.nativeElement.querySelector('[aria-label="Open profile menu"]')
      ).not.toBeNull();
    }
  );

  it('offers a mobile opener, closes after navigation, and restores desktop navigation', async () => {
    viewport.next(screen(true));
    const layout = await render();
    expect(layout.sidenav().opened).toBe(false);
    const opener = fixture.nativeElement.querySelector('button[aria-label="Open navigation menu"]');
    expect(opener).not.toBeNull();
    opener.click();
    await fixture.whenStable();
    expect(layout.sidenav().opened).toBe(true);

    navigation.next(new NavigationEnd(1, '/care', '/care'));
    await fixture.whenStable();
    expect(layout.sidenav().opened).toBe(false);
    opener.click();
    await fixture.whenStable();
    expect(layout.sidenav().opened).toBe(true);

    viewport.next(screen(false));
    await fixture.whenStable();
    expect(layout.sidenav().opened).toBe(true);
    expect(
      fixture.nativeElement.querySelector('button[aria-label="Open navigation menu"]')
    ).toBeNull();
    expect(settings.setOptions).not.toHaveBeenCalledWith(
      expect.objectContaining({ sidenavOpened: false })
    );
  });

  it('keeps the desktop collapse preference across a mobile visit and a new session', async () => {
    settings.options.sidenavCollapsed = true;
    await render();
    expect(fixture.nativeElement.querySelector('.matero-sidenav-collapsed')).not.toBeNull();
    viewport.next(screen(true));
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.matero-sidenav-collapsed')).toBeNull();
    viewport.next(screen(false));
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.matero-sidenav-collapsed')).not.toBeNull();
    fixture.destroy();
    const layout = await render();
    expect(layout.sidenav().opened).toBe(true);
    expect(fixture.nativeElement.querySelector('.matero-sidenav-collapsed')).not.toBeNull();
  });

  it('stops reacting to navigation after logout destroys the layout', async () => {
    const layout = await render();
    const scroll = vi.spyOn(layout.content(), 'scrollTo');
    fixture.destroy();
    navigation.next(new NavigationEnd(1, '/auth/login', '/auth/login'));
    await Promise.resolve();
    expect(scroll).not.toHaveBeenCalled();
  });
});
