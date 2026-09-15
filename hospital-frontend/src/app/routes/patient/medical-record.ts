import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { MatExpansionModule } from '@angular/material/expansion';
import { careLabel } from '../care/care.models';
import {
  ADMISSION_STATUS_OPTIONS,
  ADMISSION_TYPE_OPTIONS,
  SEVERITY_OPTIONS,
  Visit,
  lookupLabel,
} from './patient.models';

@Component({
  selector: 'app-medical-record',
  imports: [DatePipe, MatExpansionModule],
  templateUrl: './medical-record.html',
  styleUrl: './medical-record.scss',
})
export class MedicalRecord {
  readonly record = input.required<Visit>();
  readonly label = careLabel;
  readonly lookup = lookupLabel;
  readonly admissionTypes = ADMISSION_TYPE_OPTIONS;
  readonly admissionStatuses = ADMISSION_STATUS_OPTIONS;
  readonly severities = SEVERITY_OPTIONS;

  resultLabel(results: { revision: number }[], revision: number): string {
    return results.some(r => r.revision > revision)
      ? 'Earlier version'
      : 'Latest published version';
  }
}
