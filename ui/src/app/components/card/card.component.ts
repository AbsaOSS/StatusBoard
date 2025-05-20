import { Component, input } from '@angular/core';
import { ServiceCard, ServiceCardWithHistory } from '../../models/service-card';
import { faBug, faCircleQuestion, faCircleInfo, faHouseChimney, faBook, faSitemap } from '@fortawesome/free-solid-svg-icons';
import { faGithub } from '@fortawesome/free-brands-svg-icons';
import { DependencyGraphComponent } from '../dependency-graph/dependency-graph.component';
import { CpsDialogService } from 'cps-ui-kit';
import { CardService } from '../../services/card/card.service';
import { RefreshService } from '../../services/refresh/refresh.service';
import { CardDetailsComponent } from '../card-details/card-details.component';

@Component({
  selector: 'app-service-card',
  templateUrl: './card.component.html',
  styleUrls: ['./card.component.scss'],
  standalone: false,
})
export class CardComponent {
  card = input<ServiceCard>();
  showButtons = input<boolean>(false);
  showHistoryBar = input(false, {
    transform: (val: boolean): boolean => {
      if (val && !('history' in this.card()!)) {
        // eslint-disable-next-line no-undef
        console.warn('showHistoryBar is true but card does not have history');
      }
      return val;
    },
  });

  icoDetails = faCircleInfo;
  icoDependencies = faSitemap;
  icoHome = faHouseChimney;
  icoSnow = faBug;
  icoSupport = faCircleQuestion;
  icoDocs = faBook;
  icoGitHub = faGithub;

  private historyLen: number = 10;

  constructor(
    private dialogService: CpsDialogService,
    private cardService: CardService,
    private refreshService: RefreshService
  ) {}

  showDependencyGraph() {
    const env = this.card()!.configuration.env;
    const serviceName = this.card()!.configuration.name;
    this.refreshService.refreshForGraph(env, serviceName).subscribe(() => {
      this.dialogService.open(DependencyGraphComponent, {
        data: {
          dependenciesCards: this.cardService.getDependenciesGraphCards(env, serviceName),
          dependentsCards: this.cardService.getDependentsGraphCards(env, serviceName),
          historyCards: this.cardService.getHistoryGraphCards(env, serviceName),
        },
        headerTitle: `${env}: ${serviceName}`,
        blurredBackground: true,
        maxHeight: '80%',
      });
    });
  }

  showDetails() {
    const env = this.card()!.configuration.env;
    const serviceName = this.card()!.configuration.name;
    this.dialogService.open(CardDetailsComponent, {
      data: { card: this.card()! },
      headerTitle: `${env}: ${serviceName} details`,
      blurredBackground: true,
      maxHeight: '80%',
      maxWidth: '80%',
    });
  }

  history(): ServiceCard[] {
    const card = this.card()!;
    if (!this.isCardWithHistory(card)) {
      return [];
    }
    return card.history.slice(0, this.historyLen);
  }

  historyFill(): null[] {
    return Array(this.historyLen - this.history().length);
  }

  private isCardWithHistory(card: ServiceCard): card is ServiceCardWithHistory {
    return 'history' in card;
  }
}
