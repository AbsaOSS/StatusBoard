import { NotificationCondition } from './notification-condition';
import { NotificationAction } from './notification-action';
import { StatusCheckAction } from './status-check-action';

export interface ServiceConfigurationLinks {
  home: string;
  snow: string;
  support: string;
  documentation: string;
  github: string;
}

export interface ServiceConfiguration {
  name: string; // Part of composite PK
  env: string; // Part of composite PK
  hidden: boolean;
  snowID: string;
  description: string;
  maintenanceMessage: string;
  links: ServiceConfigurationLinks;
  statusCheckAction: StatusCheckAction;
  statusCheckIntervalSeconds: number;
  statusCheckNonGreenIntervalSeconds: number;
  notificationCondition: NotificationCondition;
  notificationAction: NotificationAction[];
}
