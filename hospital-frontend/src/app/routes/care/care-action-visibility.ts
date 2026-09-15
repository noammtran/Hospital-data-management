import { CareDetail, CareTask } from './care.models';

const CLOSED_TASKS = ['COMPLETED', 'CANCELLED', 'DECLINED'];
export const TASK_EXCEPTION_ACTIONS = ['CANCEL_ORDER', 'DECLINE_ORDER', 'RESOLVE_ORDER'];
export const VISIT_EXCEPTION_ACTIONS = ['ROUTE', 'CANCEL_VISIT', 'CANCEL_ADMISSION', 'HANDOFF'];

// These are presentation filters over server-authorized actions, never permission grants.
export function arrivalChoices(task: CareTask): string[] {
  if (
    !task.actions.includes('ARRIVAL') ||
    CLOSED_TASKS.includes(task.status) ||
    task.status === 'CANCEL_REQUESTED' ||
    (task.kind === 'ORDER' && ['AWAITING_VALIDATION', 'RESULT_READY'].includes(task.status))
  )
    return [];
  switch (task.arrival) {
    case 'EXPECTED':
      return ['ARRIVED', 'ABSENT'];
    case 'ABSENT':
      return ['ARRIVED'];
    case 'LEFT':
      return ['ARRIVED'];
    case 'ARRIVED':
      return task.status === 'IN_PROGRESS' ? ['LEFT'] : ['CALLED', 'ABSENT', 'LEFT'];
    case 'CALLED':
      return task.status === 'IN_PROGRESS' ? ['LEFT'] : ['ABSENT', 'LEFT'];
    default:
      return [];
  }
}

export function canStartTask(task: CareTask): boolean {
  return (
    task.actions.includes('START') &&
    ['ARRIVED', 'CALLED'].includes(task.arrival) &&
    (task.status === 'QUEUED' || (task.kind === 'EXAM' && task.status === 'WAITING_RESULTS'))
  );
}

export function taskClinicalActions(task: CareTask): string[] {
  // Results may be recorded and reviewed after the patient has left the service point.
  return task.actions.filter(action => ['RESULT', 'PUBLISH', 'ACKNOWLEDGE'].includes(action));
}

export function taskExceptionActions(task: CareTask): string[] {
  return task.actions.filter(action => TASK_EXCEPTION_ACTIONS.includes(action));
}

export function visitClinicalActions(detail: CareDetail): string[] {
  const { visit } = detail;
  const atCarePoint =
    visit.locationPointId !== null &&
    [visit.pointId, visit.wardPointId].includes(visit.locationPointId);
  return detail.actions.filter(action => {
    if (VISIT_EXCEPTION_ACTIONS.includes(action)) return false;
    if (action === 'NOTE') return atCarePoint;
    if (action === 'WARD_ARRIVAL') return visit.locationPointId !== visit.wardPointId;
    return visit.status !== 'WAITING';
  });
}

export function visitExceptionActions(detail: CareDetail): string[] {
  return detail.actions.filter(action => VISIT_EXCEPTION_ACTIONS.includes(action));
}

export function taskStepHint(task: CareTask): string {
  if (CLOSED_TASKS.includes(task.status) || task.status === 'CANCEL_REQUESTED') return '';
  if (task.kind === 'ORDER' && ['AWAITING_VALIDATION', 'RESULT_READY'].includes(task.status))
    return 'The service has been performed. Result validation and review can continue after the patient leaves.';
  if (task.arrival === 'EXPECTED')
    return 'Confirm arrival at this point before calling or starting.';
  if (task.arrival === 'ABSENT') return 'Patient marked absent. Confirm arrival when they return.';
  if (task.arrival === 'LEFT')
    return 'Patient has left this point. Confirm arrival again when they return.';
  if (task.status === 'IN_PROGRESS')
    return 'Work is already in progress. You do not need to start it again.';
  if (task.arrival === 'CALLED') return 'Patient called. The assigned clinician can now start.';
  if (task.arrival === 'ARRIVED')
    return 'Patient is present. Call the patient or start when ready.';
  return '';
}
