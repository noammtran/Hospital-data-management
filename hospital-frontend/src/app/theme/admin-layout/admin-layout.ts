import { BidiModule } from '@angular/cdk/bidi';
import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, ViewEncapsulation, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenav, MatSidenavContent, MatSidenavModule } from '@angular/material/sidenav';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { NgProgressbar } from 'ngx-progressbar';
import { NgProgressRouter } from 'ngx-progressbar/router';
import { filter } from 'rxjs';

import { SettingsService } from '@core';
import { SidebarNotice } from '../sidebar-notice/sidebar-notice';
import { Sidebar } from '../sidebar/sidebar';

const MOBILE_MEDIAQUERY = 'screen and (max-width: 599px)';
const MONITOR_MEDIAQUERY = 'screen and (min-width: 600px)';

@Component({
  selector: 'app-admin-layout',
  templateUrl: './admin-layout.html',
  styleUrl: './admin-layout.scss',
  encapsulation: ViewEncapsulation.None,
  imports: [
    RouterOutlet,
    BidiModule,
    MatSidenavModule,
    MatButtonModule,
    MatIconModule,
    NgProgressbar,
    NgProgressRouter,
    Sidebar,
    SidebarNotice,
  ],
})
export class AdminLayout {
  readonly sidenav = viewChild.required<MatSidenav>('sidenav');
  readonly content = viewChild.required<MatSidenavContent>('content');

  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly router = inject(Router);
  private readonly settings = inject(SettingsService);

  // Desktop collapse is a preference. Mobile visibility is temporary and must
  // not hide navigation on desktop or carry over to the next signed-in account.
  readonly collapsed = signal(this.settings.options.sidenavCollapsed);
  readonly mobileSidenavOpened = signal(false);
  private readonly isMobileScreen = signal(false);

  get isOver() {
    return this.isMobileScreen();
  }

  constructor() {
    this.breakpointObserver
      .observe([MOBILE_MEDIAQUERY, MONITOR_MEDIAQUERY])
      .pipe(takeUntilDestroyed())
      .subscribe(state => {
        this.isMobileScreen.set(state.breakpoints[MOBILE_MEDIAQUERY]);
        this.mobileSidenavOpened.set(false);
      });

    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntilDestroyed()
      )
      .subscribe(() => {
        this.mobileSidenavOpened.set(false);
        this.content().scrollTo({ top: 0 });
      });
  }

  toggleCollapsed() {
    const next = !this.collapsed();
    this.collapsed.set(next);
    this.settings.setOptions({ sidenavCollapsed: next });
  }

  onSidenavOpenedChange(isOpened: boolean) {
    // Material emits openedChange asynchronously. Ignore an older animation's
    // event if navigation or a breakpoint has already changed the drawer.
    if (this.isOver && isOpened === this.sidenav().opened) {
      this.mobileSidenavOpened.set(isOpened);
    }
  }
}
