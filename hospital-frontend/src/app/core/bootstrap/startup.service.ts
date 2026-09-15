import { Injectable, inject } from '@angular/core';
import { AuthService, User } from '@core/authentication';
import { NgxPermissionsService, NgxRolesService } from 'ngx-permissions';
import { catchError, of, switchMap, tap } from 'rxjs';
import { Menu, MenuService } from './menu.service';

@Injectable({
  providedIn: 'root',
})
export class StartupService {
  private readonly authService = inject(AuthService);
  private readonly menuService = inject(MenuService);
  private readonly permissonsService = inject(NgxPermissionsService);
  private readonly rolesService = inject(NgxRolesService);
  private loadPromise?: Promise<void>;

  /**
   * Load the application only after get the menu or other essential informations
   * such as permissions and roles.
   */
  load() {
    if (this.loadPromise) {
      return this.loadPromise;
    }

    this.loadPromise = new Promise<void>(resolve => {
      let initialLoad = true;
      this.authService
        .change()
        .pipe(
          tap(user => this.setPermissions(user)),
          switchMap(() => this.authService.menu().pipe(catchError(() => of([])))),
          tap(menu => this.setMenu(menu))
        )
        .subscribe({
          next: () => {
            if (initialLoad) {
              initialLoad = false;
              resolve();
            }
          },
          error: () => {
            if (initialLoad) {
              initialLoad = false;
              resolve();
            }
          },
        });

      this.authService.restore().subscribe();
    });

    return this.loadPromise;
  }

  private setMenu(menu: Menu[]) {
    this.menuService.addNamespace(menu, 'menu');
    this.menuService.set(menu);
  }

  private setPermissions(user: User) {
    const permissions = user.permissions ?? [];
    const roles = user.roles ?? [];
    this.permissonsService.loadPermissions(permissions);
    this.rolesService.flushRoles();
    if (roles.length > 0) {
      this.rolesService.addRoles(
        roles.reduce<Record<string, string[]>>((result, role) => {
          result[role] = permissions;
          return result;
        }, {})
      );
    }
  }
}
