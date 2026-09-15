import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { PatientPortal } from './patient-portal';
import { PatientPortalService } from './patient-portal.service';

describe('PatientPortal', () => {
  let fixture: ComponentFixture<PatientPortal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PatientPortal],
      providers: [
        {
          provide: PatientPortalService,
          useValue: {
            getMyRecord: () =>
              of({
                patient: {
                  id: 'P26MB001',
                  name: 'Nguyễn An',
                  birthDate: '1990-01-01',
                  gender: 1,
                  address: 'Hà Nội',
                  phoneNo: '0900000000',
                  email: null,
                  identityNo: '079123456789',
                  bloodType: 1,
                  allergies: null,
                  medicalHistory: null,
                  insuranceNo: null,
                  insuranceExpiry: null,
                  emergencyContactName: null,
                  emergencyContactPhone: null,
                  emergencyContactRelation: null,
                  status: 2,
                  note: null,
                },
                records: [
                  {
                    admissionId: 'A-1',
                    admissionDate: '2026-08-01',
                    dischargeDate: null,
                    departmentName: 'Nội khoa',
                    doctorName: 'Bác sĩ Demo',
                    roomName: 'P-101',
                    reason: 'Theo dõi',
                    type: 2,
                    status: 1,
                    notes: null,
                    diagnoses: [],
                    prescriptions: [],
                    care: null,
                  },
                ],
              }),
          },
        },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(PatientPortal);
    await fixture.whenStable();
  });

  afterEach(() => fixture.destroy());

  it('shows the unified self record without clinical write controls', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('Nguyễn An');
    expect(element.textContent).toContain('Nội khoa');
    expect(element.textContent).toContain('Read only');
    expect(element.querySelector('button')).toBeNull();
    expect(element.textContent).toContain('Examination and treatment record');
    expect(element.textContent).toContain('after this episode is completed');
  });
});
