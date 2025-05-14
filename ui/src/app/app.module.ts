import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule } from '@angular/router';
import { AppComponent } from './app.component';
import { StatusHistoryComponent } from './components/status-history/status-history.component';
import { StatusListComponent } from './components/status-list/status-list.component';
import { StatusNotificationComponent } from './components/status-notification/status-notification.component';
import { SharedModule } from './modules/shared/shared.module';
import { RepositoryService } from './services/repository/repository.service';
import { CardService } from './services/card/card.service';
import { BackendService } from './services/backend/backend.service';
import { RefreshService } from './services/refresh/refresh.service';
import { DependencyGraphComponent } from './components/dependency-graph/dependency-graph.component';
import { CpsLoaderComponent } from 'cps-ui-kit';
import { CardComponent } from './components/card/card.component';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { CardDetailsComponent } from './components/card-details/card-details.component';

@NgModule({
  declarations: [
    AppComponent,
    StatusListComponent,
    StatusNotificationComponent,
    StatusHistoryComponent,
    DependencyGraphComponent,
    CardDetailsComponent,
    CardComponent,
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule.forRoot([]),
    SharedModule,
    FontAwesomeModule,
    CpsLoaderComponent,
  ],
  providers: [BackendService, CardService, RefreshService, RepositoryService],
  bootstrap: [AppComponent],
})
export class AppModule {}
