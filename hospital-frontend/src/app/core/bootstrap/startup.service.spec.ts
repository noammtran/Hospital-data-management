import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TokenService } from '@core/authentication';
import { MenuService } from '@core/bootstrap/menu.service';
import { StartupService } from '@core/bootstrap/startup.service';
import { LocalStorageService, MemoryStorageService } from '@shared/services/storage.service';
import { NgxPermissionsModule, NgxPermissionsService, NgxRolesService } from 'ngx-permissions';
import { vi } from 'vitest';

describe('StartupService', () => {
  let httpMock: HttpTestingController;
  let startup: StartupService;
  let tokenService: TokenService;
  let menuService: MenuService;
  let permissionsService: NgxPermissionsService;
  let rolesService: NgxRolesService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NgxPermissionsModule.forRoot()],
      providers: [
        { provide: LocalStorageService, useClass: MemoryStorageService },
        {
          provide: NgxPermissionsService,
          useValue: { loadPermissions: (_permissions: string[]) => undefined },
        },
        {
          provide: NgxRolesService,
          useValue: {
            flushRoles: () => undefined,
            addRoles: (_roles: Record<string, string[]>) => undefined,
          },
        },
        StartupService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
    startup = TestBed.inject(StartupService);
    tokenService = TestBed.inject(TokenService);
    menuService = TestBed.inject(MenuService);
    permissionsService = TestBed.inject(NgxPermissionsService);
    rolesService = TestBed.inject(NgxRolesService);
  });

  afterEach(() => httpMock.verify());

  it('should load server permissions, roles and permission-aware menu for a stored session', async () => {
    const menuData = { menu: [] };
    const permissions = ['VIEW_PATIENT', 'VIEW_STATISTICS'];
    tokenService.set({ access_token: 'token', token_type: 'Bearer' });
    vi.spyOn(menuService, 'addNamespace');
    vi.spyOn(menuService, 'set');
    vi.spyOn(permissionsService, 'loadPermissions');
    vi.spyOn(rolesService, 'flushRoles');
    vi.spyOn(rolesService, 'addRoles');

    const loading = startup.load();
    httpMock.expectOne('/api/v1/auth/me').flush({
      success: true,
      statusCode: 200,
      message: 'Success',
      data: {
        id: 2,
        username: 'doctor',
        email: 'doctor@hospital.example',
        displayName: 'Doctor Demo',
        role: 'DOCTOR',
        permissions,
      },
      timestamp: '2026-07-31T00:00:00Z',
    });
    httpMock.expectOne(request => request.url.startsWith('data/menu.json')).flush(menuData);
    await loading;

    expect(menuService.addNamespace).toHaveBeenCalledWith(menuData.menu, 'menu');
    expect(menuService.set).toHaveBeenCalledWith(menuData.menu);
    expect(permissionsService.loadPermissions).toHaveBeenCalledWith(permissions);
    expect(rolesService.flushRoles).toHaveBeenCalled();
    expect(rolesService.addRoles).toHaveBeenCalledWith({ DOCTOR: permissions });
  });

  it('should clear permissions and menu when no token exists', async () => {
    vi.spyOn(menuService, 'set');
    vi.spyOn(permissionsService, 'loadPermissions');
    vi.spyOn(rolesService, 'flushRoles');

    await startup.load();

    httpMock.expectNone('/api/v1/auth/me');
    expect(menuService.set).toHaveBeenCalledWith([]);
    expect(permissionsService.loadPermissions).toHaveBeenCalledWith([]);
    expect(rolesService.flushRoles).toHaveBeenCalled();
  });
});
