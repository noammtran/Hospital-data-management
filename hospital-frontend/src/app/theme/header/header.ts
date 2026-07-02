import { Component, ViewEncapsulation, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';

import { UserButton } from '../widgets/user-button';

@Component({
  selector: 'app-header',
  templateUrl: './header.html',
  styleUrl: './header.scss',
  host: {
    class: 'matero-header',
  },
  encapsulation: ViewEncapsulation.None,
  imports: [MatToolbarModule, MatButtonModule, MatIconModule, UserButton],
})
export class Header {
  readonly showToggle = input(true);

  readonly toggleSidenav = output<void>();
  // Kept so the admin layout's binding still resolves; the notice drawer is no
  // longer reachable from the header.
  readonly toggleSidenavNotice = output<void>();
}
