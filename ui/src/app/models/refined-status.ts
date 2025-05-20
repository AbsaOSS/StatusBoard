import { RawStatus } from './raw-status';

export interface RefinedStatus {
  serviceName: string;
  env: string;
  status: RawStatus;
  maintenanceMessage: string;
  firstSeen: string; // format: date-time
  lastSeen: string; // format: date-time
  notificationSent: boolean;
}
