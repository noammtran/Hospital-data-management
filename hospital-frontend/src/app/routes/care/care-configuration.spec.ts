import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { CareConfiguration } from './care-configuration';
import { CareOptions } from './care.models';
import { CareService } from './care.service';
import { UserService } from '../user/user.service';
import { StaffWorkProfile } from '../user/user.models';

describe('CareConfiguration staff onboarding', () => {
  const options: CareOptions = {
    currentUserId: 1,
    points: [
      {
        id: 101,
        code: 'CLINIC_101',
        name: 'Phòng khám 101',
        kind: 'CLINIC',
        departmentId: 1,
        active: true,
      },
    ],
    staff: [
      { id: 7, name: 'Nguyễn Minh An', role: 'DOCTOR', doctorId: null, active: true },
      { id: 8, name: 'Trần Minh Bình', role: 'DOCTOR', doctorId: 42, active: true },
    ],
    assignments: [{ id: 1, userId: 8, pointId: 101, perform: true, verify: false, active: true }],
    doctors: [
      { id: 42, name: 'Trần Minh Bình' },
      { id: 43, name: 'Nguyễn Minh An' },
    ],
    departments: [{ id: 1, name: 'Khoa Nội' }],
    services: [],
    rooms: [],
  };
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
    rolePermissions: ['CARE_VIEW', 'CARE_EXAMINE', 'CARE_PERFORM', 'CARE_VERIFY'],
    setupIssues: ['DOCTOR_LINK_MISSING', 'POINT_ASSIGNMENT_MISSING'],
  };
  const doctor = {
    id: 1000,
    code: 'DOC_TEST',
    name: 'Nguyễn Minh An',
    specialization: 'Nội',
    departmentId: 1,
    departmentName: 'Khoa Nội',
    active: true,
  };
  const care = { options: vi.fn(), configure: vi.fn() };
  const users = { workProfile: vi.fn(), createDoctorProfile: vi.fn() };

  beforeEach(() => {
    vi.clearAllMocks();
    care.options.mockReturnValue(of(options));
    care.configure.mockReturnValue(of(options));
    users.workProfile.mockReturnValue(of(profile));
    users.createDoctorProfile.mockReturnValue(of(doctor));
    TestBed.configureTestingModule({
      imports: [CareConfiguration],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { queryParamMap: of(convertToParamMap({ userId: '7' })) },
        },
        { provide: CareService, useValue: care },
        { provide: UserService, useValue: users },
      ],
    });
  });

  it('preselects the staff from navigation and excludes other accounts linked doctor profiles', async () => {
    const fixture = TestBed.createComponent(CareConfiguration);
    await fixture.whenStable();
    const component = fixture.componentInstance;
    expect(component.assignmentForm.controls.userId.value).toBe(7);
    expect(users.workProfile).toHaveBeenCalledWith(7);
    expect(component.visibleAssignments()).toEqual([]);
    expect(component.availableDoctors().map(d => d.id)).toEqual([43]);
    expect(fixture.nativeElement.textContent).toContain('Work setup for Nguyễn Minh An');
    expect(fixture.nativeElement.querySelector('a[href="/users?staffId=7"]')).not.toBeNull();
  });

  it('creates a doctor for the selected account and then submits its point assignment', async () => {
    const fixture = TestBed.createComponent(CareConfiguration);
    await fixture.whenStable();
    const component = fixture.componentInstance;
    component.startDoctorProfile();
    expect(component.doctorForm.controls.name.value).toBe('Nguyễn Minh An');
    component.doctorForm.patchValue({ code: 'DOC_TEST', departmentId: 1, specialization: ' Nội ' });
    component.createDoctorProfile();
    await fixture.whenStable();
    expect(users.createDoctorProfile).toHaveBeenCalledWith(7, {
      code: 'DOC_TEST',
      name: 'Nguyễn Minh An',
      departmentId: 1,
      specialization: 'Nội',
      phoneNo: null,
      email: null,
    });
    expect(component.assignmentForm.controls.doctorId.value).toBe(1000);
    expect(component.selectedStaff()?.doctorId).toBe(1000);
    expect(component.showDoctorForm()).toBe(false);
    expect(care.configure).not.toHaveBeenCalled();
    component.assignmentForm.patchValue({ pointId: 101, perform: true });
    component.save('assignments');
    expect(care.configure).toHaveBeenCalledWith(
      'assignments',
      {
        userId: 7,
        pointId: 101,
        perform: true,
        verify: false,
        active: true,
        doctorId: 1000,
      },
      undefined
    );
  });

  it('validates a new doctor and preserves the form when the backend rejects its code', async () => {
    const fixture = TestBed.createComponent(CareConfiguration);
    await fixture.whenStable();
    const component = fixture.componentInstance;
    component.startDoctorProfile();
    await fixture.whenStable();
    component.createDoctorProfile();
    expect(users.createDoctorProfile).not.toHaveBeenCalled();
    component.doctorForm.patchValue({ code: 'DOC_TEST', departmentId: 1 });
    users.createDoctorProfile.mockReturnValueOnce(
      throwError(() => ({ error: { errors: { code: 'Doctor code is already used' } } }))
    );
    component.createDoctorProfile();
    await fixture.whenStable();
    expect(component.doctorForm.controls.code.getError('server')).toBe(
      'Doctor code is already used'
    );
    expect(component.showDoctorForm()).toBe(true);
    expect(component.selectedStaff()?.doctorId).toBeNull();
    expect(component.assignmentForm.enabled).toBe(true);
    expect(care.configure).not.toHaveBeenCalled();
  });

  it('blocks unsupported validation and unlinked examination assignments', async () => {
    const fixture = TestBed.createComponent(CareConfiguration);
    await fixture.whenStable();
    const component = fixture.componentInstance;
    component.assignmentForm.patchValue({ pointId: 101, verify: true });
    component.save('assignments');
    expect(component.error()).toContain('service point');
    component.assignmentForm.patchValue({ verify: false, perform: true });
    component.save('assignments');
    expect(component.error()).toContain('Link an existing doctor profile');
    expect(care.configure).not.toHaveBeenCalled();
  });

  it('clears doctor creation and cancels stale profile reads when staff selection changes', async () => {
    const fixture = TestBed.createComponent(CareConfiguration);
    await fixture.whenStable();
    const component = fixture.componentInstance;
    component.startDoctorProfile();
    const pending = new Subject<StaffWorkProfile>();
    users.workProfile.mockReturnValueOnce(pending);
    component.assignmentForm.controls.userId.setValue(8);
    expect(component.showDoctorForm()).toBe(false);
    expect(component.assignmentForm.controls.doctorId.value).toBe(42);
    component.assignmentForm.controls.userId.setValue(7);
    pending.next({ ...profile, rolePermissions: ['MANAGE_USERS'] });
    expect(component.workProfile()?.rolePermissions).not.toContain('MANAGE_USERS');
    expect(component.profileLoading()).toBe(false);
  });

  it('keeps account selection locked during doctor creation and prevents duplicate requests', async () => {
    const fixture = TestBed.createComponent(CareConfiguration);
    await fixture.whenStable();
    const component = fixture.componentInstance;
    component.startDoctorProfile();
    component.doctorForm.patchValue({ code: 'DOC_TEST', departmentId: 1 });
    const pending = new Subject<typeof doctor>();
    users.createDoctorProfile.mockReturnValue(pending);
    component.createDoctorProfile();
    component.createDoctorProfile();
    expect(users.createDoctorProfile).toHaveBeenCalledTimes(1);
    expect(component.assignmentForm.controls.userId.disabled).toBe(true);
    pending.next(doctor);
    pending.complete();
    expect(component.assignmentForm.controls.userId.enabled).toBe(true);
    expect(component.assignmentForm.controls.doctorId.value).toBe(1000);
  });
});
