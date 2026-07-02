import { Routes } from '@angular/router';
import { authGuard } from '@core';
import { AdminLayout } from '@theme/admin-layout/admin-layout';
import { AuthLayout } from '@theme/auth-layout/auth-layout';
import { Error403 } from './routes/sessions/error-403';
import { Error404 } from './routes/sessions/error-404';
import { Error500 } from './routes/sessions/error-500';
import { Login } from './routes/sessions/login/login';
import { Register } from './routes/sessions/register/register';

export const routes: Routes = [
  {
    path: '',
    component: AdminLayout,
    canActivate: [authGuard],
    canActivateChild: [authGuard],
    children: [
      { path: '', redirectTo: 'patients', pathMatch: 'full' },
      {
        path: 'patients',
        loadComponent: () => import('./routes/patient/patient-list').then(m => m.PatientList),
      },
      {
        path: 'patient-history',
        loadComponent: () =>
          import('./routes/patient/patient-history-page').then(m => m.PatientHistoryPage),
      },
      {
        path: 'statistics',
        loadComponent: () => import('./routes/statistics/statistics').then(m => m.Statistics),
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
      { path: 'register', component: Register },
    ],
  },
  { path: '**', redirectTo: 'patients' },
];
