import { animate, state, style, transition, trigger } from '@angular/animations';
import { Component, input, OnDestroy, output } from '@angular/core';
import { CpsNotificationAppearance } from 'cps-ui-kit';

export enum NotificationPosition {
  CENTER = 'center',
  LEFT = 'left',
  RIGHT = 'right',
}

export type NotificationType = 'success' | 'error' | 'warning' | 'info';

export const NotificationTypeValues: Record<Capitalize<NotificationType>, NotificationType> = {
  Success: 'success' as NotificationType,
  Error: 'error' as NotificationType,
  Warning: 'warning' as NotificationType,
  Info: 'info' as NotificationType,
};

export interface NotificationStatus {
  message: string;
  details?: string;
  type: NotificationType;
}

export interface NotificationConfig {
  position?: NotificationPosition;
  appearance?: CpsNotificationAppearance;
  timeout?: number;
  maxWidth?: string;
  closable?: boolean;
  refreshable?: boolean;
  refreshDisabled?: boolean;
}

@Component({
  selector: 'app-notification',
  templateUrl: './status-notification.component.html',
  styleUrls: ['./status-notification.component.scss'],
  animations: [
    trigger('notificationState', [
      state('visible', style({ opacity: 1 })),
      transition('void => *', [style({ opacity: 0 }), animate('200ms ease-out')]),
      transition('* => void', [animate('200ms ease-in', style({ opacity: 0 }))]),
    ]),
  ],
  standalone: false,
})
export class StatusNotificationComponent implements OnDestroy {
  config = input<NotificationConfig>({
    closable: false,
    refreshable: true,
    appearance: CpsNotificationAppearance.FILLED,
    maxWidth: '900px',
  });
  status = input<NotificationStatus>();
  refresh = input<boolean>(false);
  closed = output();
  refreshed = output();

  timeout: number | null = null;

  constructor() {}

  get filled() {
    return this.config()?.appearance === CpsNotificationAppearance.FILLED;
  }

  get color() {
    return this.getColor(this.status()?.type);
  }

  get position() {
    return this.config()?.position ?? NotificationPosition.CENTER;
  }

  ngOnDestroy() {
    this.clearTimeout();
  }

  close() {
    this.clearTimeout();
    this.closed.emit();
  }

  onClickRefreshButton() {
    this.refreshed.emit();
  }

  clearTimeout() {
    if (this.timeout) {
      // eslint-disable-next-line no-undef
      clearTimeout(this.timeout);
      this.timeout = null;
    }
  }

  private getColor(type: NotificationType | undefined): string {
    switch (type) {
      case NotificationTypeValues.Warning:
        return 'warn';
      case NotificationTypeValues.Success:
        return 'success';
      case NotificationTypeValues.Info:
        return 'info';
      default:
        return 'error';
    }
  }
}
