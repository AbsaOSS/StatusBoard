import { DatePipe } from '@angular/common';
import { Injectable } from '@angular/core';
import { ServiceCard } from '../../models/service-card';
import { RawStatusColor } from '../../models/raw-status';
import { RefinedStatus } from '../../models/refined-status';
import { ServiceConfiguration } from '../../models/service-configuration';

@Injectable({
  providedIn: 'root',
})
export class Utils {
  makeCard(configuration: ServiceConfiguration, status: RefinedStatus): ServiceCard {
    return {
      id: `${status.env}_${status.serviceName}_${status.firstSeen}`,
      configuration: configuration,
      status: status,
      derived: {
        icon: this.getIcon(status.status.color),
        cssColor: this.getCssColor(status.status.color),
        timeFrame: this.getTimeFrame(status),
        timeFrameToolTip: this.getTimeFrameToolTip(status),
      },
    };
  }

  getIcon(color: RawStatusColor): string {
    switch (color) {
      case RawStatusColor.GREEN:
        return 'toast-success';
      case RawStatusColor.AMBER:
        return 'toast-warning';
      case RawStatusColor.BLACK:
        return 'toast-info';
      case RawStatusColor.RED:
      default:
        return 'toast-error';
    }
  }

  getCssColor(color: RawStatusColor): string {
    switch (color) {
      case RawStatusColor.GREEN:
        return 'var(--cps-color-success)';
      case RawStatusColor.AMBER:
        return 'var(--cps-color-warn)';
      case RawStatusColor.BLACK:
        return 'var(--cps-color-graphite)';
      case RawStatusColor.RED:
      default:
        return 'var(--cps-color-error)';
    }
  }

  getDurationString(firstSeen: Date, lastSeen: Date): string {
    const durationTotalMinutes = Math.floor((lastSeen.getTime() - firstSeen.getTime()) / 1000 / 60);
    const durationTotalHours = Math.floor(durationTotalMinutes / 60);
    const durationTotalDays = Math.floor(durationTotalHours / 24);

    const durationMinutes = durationTotalMinutes % 60;
    const durationHours = durationTotalHours % 24;
    const durationDays = durationTotalDays;

    const durationMinutesStr = durationMinutes == 1 ? '1 minute' : `${durationMinutes} minutes`;
    const durationHoursStr = durationHours == 1 ? '1 hour' : `${durationHours} hours`;
    const durationDaysStr = durationDays == 1 ? '1 day' : `${durationDays} days`;

    return durationDays > 0
      ? `${durationDaysStr} ${durationHoursStr} ${durationMinutesStr}`
      : durationHours > 0
        ? `${durationHoursStr} ${durationMinutesStr}`
        : `${durationMinutesStr}`;
  }

  getTimeFrame(status: RefinedStatus): string {
    const datePipe = new DatePipe('en-US');
    let formatTime = function (date: Date): string {
      return datePipe.transform(date, "yyyy-MM-dd HH:mm 'UTC'", 'UTC')!;
    };

    const firstSeen = new Date(status.firstSeen);
    const firstSeenStr = formatTime(firstSeen);

    const lastSeen = new Date(status.lastSeen);
    const lastSeenStr = formatTime(lastSeen);

    const duration = this.getDurationString(firstSeen, lastSeen);

    return `${firstSeenStr} - ${lastSeenStr} [${duration}]`;
  }

  getTimeFrameToolTip(status: RefinedStatus): string {
    const datePipe = new DatePipe('en-US');
    let formatTime = function (date: Date): string {
      return datePipe.transform(date, "yyyy-MM-dd HH:mm 'UTC'", 'UTC')!;
    };

    const firstSeen = new Date(status.firstSeen);
    const firstSeenStr = formatTime(firstSeen);

    const lastSeen = new Date(status.lastSeen);
    const lastSeenStr = formatTime(lastSeen);

    const duration = this.getDurationString(firstSeen, lastSeen);

    return `First seen in this state: ${firstSeenStr} <br>Last seen in this state: ${lastSeenStr}<br>Observed in this state for: ${duration}`;
  }

  applyFilter(filterTerm: string, cards: ServiceCard[]): ServiceCard[] {
    if (!filterTerm) return cards;

    const terms = filterTerm.toLowerCase().split(' ');
    return cards.filter(card =>
      terms.every(
        term =>
          card.configuration.env.toLowerCase().includes(term) ||
          card.configuration.name.toLowerCase().includes(term) ||
          card.configuration.description.toLowerCase().includes(term) ||
          card.status.status.toString().toLowerCase().includes(term) ||
          card.status.maintenanceMessage.toString().toLowerCase().includes(term)
      )
    );
  }
}
