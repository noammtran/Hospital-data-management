import { Routes } from '@angular/router';
import { authGuard, landingRedirect, passwordChangedGuard, permissionGuard } from '@core';
import { AdminLayout } from '@theme/admin-layout/admin-layout';
import { AuthLayout } from '@theme/auth-layout/auth-layout';
import { Error403 } from './routes/sessions/error-403';
import { Error404 } from './routes/sessions/error-404';
import { Error500 } from './routes/sessions/error-500';
import { Login } from './routes/sessions/login/login';
import { ChangePassword } from './routes/sessions/change-password/change-password';

export const routes: Routes = [
  {
    path: '',
    component: AdminLayout,
    canActivate: [authGuard, passwordChangedGuard],
    canActivateChild: [authGuard],
    children: [
      { path: '', redirectTo: landingRedirect, pathMatch: 'full' },
      {
        path: 'care', canActivate: [permissionGuard], data: { permission: 'CARE_VIEW' },
        loadComponent: () => import('./routes/care/care-worklist').then(m => m.CareWorklist),
      },
      {
        path: 'care/configuration', canActivate: [permissionGuard], data: { permission: 'CARE_MANAGE' },
        loadComponent: () => import('./routes/care/care-configuration').then(m => m.CareConfiguration),
      },
      {
        path: 'care/:id', canActivate: [permissionGuard], data: { permission: 'CARE_VIEW' },
        loadComponent: () => import('./routes/care/care-detail').then(m => m.CareDetail),
      },
      {
        path: 'my-record',
        canActivate: [permissionGuard],
        data: { permission: 'VIEW_OWN_RECORD' },
        loadComponent: () =>
          import('./routes/patient-portal/patient-portal').then(m => m.PatientPortal),
      },
      {
        path: 'patients',
        canActivate: [permissionGuard],
        data: { permission: 'VIEW_PATIENT' },
        loadComponent: () => import('./routes/patient/patient-list').then(m => m.PatientList),
      },
      {
        path: 'audit-log',
        canActivate: [permissionGuard],
        data: { permission: 'VIEW_AUDIT_LOG' },
        loadComponent: () => import('./routes/audit-log/audit-log-page').then(m => m.AuditLogPage),
      },
      { path: 'patient-history', redirectTo: 'audit-log', pathMatch: 'full' },
      {
        path: 'statistics',
        canActivate: [permissionGuard],
        data: { permission: 'VIEW_STATISTICS' },
        loadComponent: () => import('./routes/statistics/statistics').then(m => m.Statistics),
      },
      {
        path: 'users',
        canActivate: [permissionGuard],
        data: { permission: 'MANAGE_USERS' },
        loadComponent: () => import('./routes/user/user-list').then(m => m.UserList),
      },
      { path: '403', component: Error403 },
      { path: '404', component: Error404 },
      { path: '500', component: Error500 },
    ],
  },
  {
    path: 'auth',
    component: AuthLayout,
    children: [
      { path: 'login', component: Login },
      {
        path: 'change-password',
        component: ChangePassword,
        canActivate: [authGuard],
      },
    ],
  },
  { path: '**', redirectTo: landingRedirect },
];
