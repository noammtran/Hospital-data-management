import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from '@core/authentication';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { UserWorkDialog } from './user-work-dialog';
import { StaffWorkProfile } from './user.models';
import { UserService } from './user.service';

describe('UserWorkDialog', () => {
  const profile: StaffWorkProfile = {
    user: {
      id: 7,
      username: '001201100099',
      identityNo: '001201100099',
      email: 'minh.an@example.test',
      displayName: 'Nguyễn Minh An',
      role: 'DOCTOR',
      active: true,
      mustChangePassword: true,
      createdBy: 'test-admin',
      createdDate: '',
      lastUpdatedBy: 'test-admin',
      lastUpdatedDate: '',
    },
    doctor: null,
    assignments: [],
    rolePermissions: ['CARE_VIEW', 'CARE_EXAMINE'],
    setupIssues: ['DOCTOR_LINK_MISSING', 'POINT_ASSIGNMENT_MISSING'],
  };
  const users = { workProfile: vi.fn() };
  const auth = { hasPermission: vi.fn() };
  const close = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    users.workProfile.mockReturnValue(of(profile));
    auth.hasPermission.mockReturnValue(true);
    TestBed.configureTestingModule({
      imports: [UserWorkDialog],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: users },
        { provide: AuthService, useValue: auth },
        { provide: MAT_DIALOG_DATA, useValue: { userId: 7, created: true } },
        { provide: MatDialogRef, useValue: { close } },
      ],
    });
  });

  it('shows missing configuration separately from first password replacement', async () => {
    const fixture = TestBed.createComponent(UserWorkDialog);
    await fixture.whenStable();
    expect(users.workProfile).toHaveBeenCalledWith(7);
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Account created');
    expect(text).toContain('No doctor profile linked');
    expect(text).toContain('No active assignment');
    expect(text).toContain('temporary password');
    expect(text).toContain('Conduct examinations');
    expect(text).not.toContain('No work setup items pending');
  });

  it('navigates to work configuration for this account', async () => {
    const fixture = TestBed.createComponent(UserWorkDialog);
    await fixture.whenStable();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.componentInstance.setUpWork();
    expect(close).toHaveBeenCalledOnce();
    expect(navigate).toHaveBeenCalledWith(['/care/configuration'], { queryParams: { userId: 7 } });
  });

  it('keeps inactive assignments and the doctor profile readable', async () => {
    users.workProfile.mockReturnValue(
      of({
        ...profile,
        setupIssues: ['DOCTOR_UNAVAILABLE'],
        doctor: {
          id: 1000,
          code: 'DOC_TEST',
          name: 'Nguyễn Minh An',
          specialization: 'Nội',
          departmentId: 1,
          departmentName: 'Khoa Nội',
          active: false,
        },
        assignments: [
          {
            id: 1,
            pointId: 101,
            pointName: 'Phòng khám 101',
            pointKind: 'CLINIC',
            departmentName: 'Khoa Nội',
            pointActive: false,
            active: false,
            perform: true,
            verify: false,
          },
        ],
      })
    );
    const fixture = TestBed.createComponent(UserWorkDialog);
    await fixture.whenStable();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('DOC_TEST');
    expect(text).toContain('Phòng khám 101');
    expect(text).toContain('Assignment inactive');
    expect(text).toContain('Point unavailable');
    expect(text).toContain('Clinical execution: Assigned');
  });

  it('allows retry after a failed profile read without displaying stale details', async () => {
    users.workProfile.mockReturnValueOnce(
      throwError(() => ({ error: { message: 'Temporary failure' } }))
    );
    const fixture = TestBed.createComponent(UserWorkDialog);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Temporary failure');
    expect(fixture.componentInstance.profile()).toBeNull();
    fixture.componentInstance.load();
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Nguyễn Minh An');
    expect(fixture.nativeElement.textContent).not.toContain('Temporary failure');
  });

  it('does not offer or execute configuration without care management permission', async () => {
    auth.hasPermission.mockReturnValue(false);
    const fixture = TestBed.createComponent(UserWorkDialog);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).not.toContain('Set up work');
    fixture.componentInstance.setUpWork();
    expect(close).not.toHaveBeenCalled();
  });
});
