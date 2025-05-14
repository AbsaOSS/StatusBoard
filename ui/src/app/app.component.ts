import { AfterViewInit, Component, computed, model, signal, ViewChild } from '@angular/core';
import { CpsNotificationService } from 'cps-ui-kit';
import packageJson from '../../package.json';
import { StatusHistoryComponent } from './components/status-history/status-history.component';
import { StatusListComponent } from './components/status-list/status-list.component';
import { NotificationStatus, NotificationTypeValues } from './components/status-notification/status-notification.component';
import { ServiceCard } from './models/service-card';
import { CardService } from './services/card/card.service';
import { RefreshService } from './services/refresh/refresh.service';
import { RawStatusColor } from './models/raw-status';
import { ActivatedRoute, Router } from '@angular/router';
import { Utils } from './services/utils/utils';
import { environment } from '../environments/environment';
import { faPeopleGroup, faUserPlus } from '@fortawesome/free-solid-svg-icons';
import { faGithub } from '@fortawesome/free-brands-svg-icons';

const ERROR_FETCHING_DATA = 'Error fetching data';
const SUCCESS_FETCHING_DATA = 'Data fetched successfully';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  standalone: false,
})
export class AppComponent implements AfterViewInit {
  @ViewChild(StatusHistoryComponent) serviceHistoryComponent!: StatusHistoryComponent;
  @ViewChild(StatusListComponent) serviceStatusComponent!: StatusListComponent;

  protected readonly version = packageJson.version;
  protected readonly environment = environment;

  notificationStatus = signal<NotificationStatus>({
    message: 'INITIALIZING...',
    type: NotificationTypeValues.Info,
  });
  isLoading = signal(false);
  filter = model('');
  latestVisibleCards = signal<ServiceCard[]>([]);
  latestVisibleCardsFiltered = computed(() => this.utils.applyFilter(this.filter(), this.latestVisibleCards()));
  historyVisibleCards = signal<ServiceCard[]>([]);
  historyVisibleCardsFiltered = computed(() => this.utils.applyFilter(this.filter(), this.historyVisibleCards()));

  icoTeam = faPeopleGroup;
  icoJoin = faUserPlus;
  icoGitHub = faGithub;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private utils: Utils,
    private cardService: CardService,
    private refreshService: RefreshService,
    private notificationService: CpsNotificationService
  ) {}

  ngAfterViewInit() {
    this.route.queryParams.subscribe(params => {
      this.filter.set(params['filter'] || '');
    });

    this.refreshService.refreshPending$.subscribe(loading => {
      this.isLoading.set(loading);
    });

    this.cardService.getLatestVisibleCards().subscribe({
      next: latestVisibleCards => {
        this.latestVisibleCards.set(latestVisibleCards);
        this.updateNotificationStatus(latestVisibleCards);
      },
      error: error => {
        // eslint-disable-next-line no-undef
        console.error(error);
        this.updateNotificationStatusToError();
        this.notificationService.error('Failed to load latest visible service cards');
      },
    });

    this.cardService.getHistoryVisibleCards().subscribe({
      next: historyVisibleCards => {
        this.historyVisibleCards.set(historyVisibleCards);
      },
      error: error => {
        // eslint-disable-next-line no-undef
        console.error(error);
        this.updateNotificationStatusToError();
        this.notificationService.error('Failed to load history visible service cards');
      },
    });

    // Initialize data only after we've set up our subscriptions
    this.triggerInitialRefresh();
  }

  onFilterChanged(value: string) {
    this.filter.set(value ?? '');
    this.router.navigate([], {
      queryParams: { filter: this.filter() },
      queryParamsHandling: 'merge',
    });
  }

  refreshState() {
    if (this.isLoading()) return;
    this.triggerIncrementalRefresh();
  }

  private triggerIncrementalRefresh() {
    this.refreshService.incrementalRefresh().subscribe({
      complete: () => {
        this.notificationService.success(SUCCESS_FETCHING_DATA);
      },
      error: (err: Error) => {
        this.updateNotificationStatusToError();
        this.notificationService.error(`${ERROR_FETCHING_DATA}: ${err}`);
      },
    });
  }

  private triggerInitialRefresh() {
    this.refreshService.initialRefresh().subscribe({
      complete: () => {
        this.notificationService.success(SUCCESS_FETCHING_DATA);
      },
      error: (err: Error) => {
        this.updateNotificationStatusToError();
        this.notificationService.error(`${ERROR_FETCHING_DATA}: ${err}`);
      },
    });
  }

  private updateNotificationStatusToError() {
    this.notificationStatus.set({
      message: ERROR_FETCHING_DATA,
      type: NotificationTypeValues.Error,
    });
  }

  private updateNotificationStatus(latestVisibleCards: ServiceCard[]) {
    if (!latestVisibleCards)
      return this.notificationStatus.set({
        message: ERROR_FETCHING_DATA,
        type: NotificationTypeValues.Error,
      });

    const redCards = latestVisibleCards.filter(card => card.status.status.color == RawStatusColor.RED).length;
    const amberCards = latestVisibleCards.filter(card => card.status.status.color == RawStatusColor.AMBER).length;
    const greenCards = latestVisibleCards.filter(card => card.status.status.color == RawStatusColor.GREEN).length;
    const blackCards = latestVisibleCards.filter(card => card.status.status.color == RawStatusColor.BLACK).length;
    const totalCards = latestVisibleCards.length;

    let msgParts: string[] = [];
    if (redCards > 0) msgParts.push(`${redCards} Red`);
    if (amberCards > 0) msgParts.push(`${amberCards} Amber`);
    if (greenCards > 0) msgParts.push(`${greenCards} Green`);
    if (blackCards > 0) msgParts.push(`${blackCards} Black`);

    const msg = `Services: ${msgParts.join(' | ')} / ${totalCards} Total`;
    const msgType = redCards + amberCards == 0 ? NotificationTypeValues.Success : NotificationTypeValues.Warning;
    return this.notificationStatus.set({ message: msg, type: msgType });
  }
}
