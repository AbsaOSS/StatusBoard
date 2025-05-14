import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NEVER, of, throwError } from 'rxjs';
import { AppComponent } from './app.component';
import { NotificationTypeValues } from './components/status-notification/status-notification.component';
import { CardService } from './services/card/card.service';
import { RefreshService } from './services/refresh/refresh.service';
import { makeTestCard } from './test.data';

jest.mock('../../package.json', () => ({
  version: '1.0.0',
}));

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let cardServiceMock: jest.Mocked<CardService>;
  let refreshServiceMock: jest.Mocked<RefreshService>;

  beforeEach(async () => {
    refreshServiceMock = {
      initialRefresh: jest.fn(() => NEVER),
      incrementalRefresh: jest.fn(() => NEVER),
    } as never;
    Object.defineProperty(refreshServiceMock, 'refreshPending$', {
      get: jest.fn(() => NEVER),
      configurable: true,
    });

    cardServiceMock = {
      getLatestVisibleCards: jest.fn(() => NEVER),
      getHistoryVisibleCards: jest.fn(() => NEVER),
    } as never;

    await TestBed.configureTestingModule({
      declarations: [AppComponent],
      imports: [HttpClientTestingModule, RouterTestingModule],
      providers: [
        { provide: RefreshService, useValue: refreshServiceMock },
        { provide: CardService, useValue: cardServiceMock },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
  });

  it('should create the app', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with initialRefresh', fakeAsync(() => {
    component.ngAfterViewInit();
    tick();
    expect(refreshServiceMock.initialRefresh).toHaveBeenCalled();
    expect(refreshServiceMock.incrementalRefresh).not.toHaveBeenCalled();
  }));

  it('should handle error on initialRefresh', fakeAsync(() => {
    refreshServiceMock.initialRefresh.mockReturnValue(throwError(() => new Error('Test error')));
    component.ngAfterViewInit();
    tick();
    expect(component.notificationStatus().message).toBe('Error fetching data');
    expect(component.notificationStatus().type).toBe(NotificationTypeValues.Error);
  }));

  it('should handle triggered refresh via incrementalRefresh', fakeAsync(() => {
    component.refreshState();
    tick();
    expect(refreshServiceMock.initialRefresh).not.toHaveBeenCalled();
    expect(refreshServiceMock.incrementalRefresh).toHaveBeenCalled();
  }));

  it('should not trigger triggered refresh in onLoad', fakeAsync(() => {
    Object.defineProperty(refreshServiceMock, 'refreshPending$', {
      get: jest.fn(() => of(true)),
    });
    component.ngAfterViewInit();
    tick();
    component.refreshState();
    tick();
    expect(refreshServiceMock.incrementalRefresh).not.toHaveBeenCalled();
  }));

  it('should handle error on incrementalRefresh', fakeAsync(() => {
    refreshServiceMock.incrementalRefresh.mockReturnValue(throwError(() => new Error('Test error')));
    component.refreshState();
    tick();
    expect(component.notificationStatus().message).toBe('Error fetching data');
    expect(component.notificationStatus().type).toBe(NotificationTypeValues.Error);
  }));

  it('should receive latest visible cards', fakeAsync(() => {
    const testCard = makeTestCard();
    cardServiceMock.getLatestVisibleCards.mockReturnValue(of([testCard]));
    component.ngAfterViewInit();
    tick();
    expect(component.latestVisibleCards()).toEqual([testCard]);
  }));

  it('should handle latest visible cards error', fakeAsync(() => {
    cardServiceMock.getLatestVisibleCards.mockReturnValue(throwError(() => new Error('Test error')));
    component.ngAfterViewInit();
    tick();
    expect(component.notificationStatus().message).toBe('Error fetching data');
    expect(component.notificationStatus().type).toBe(NotificationTypeValues.Error);
  }));

  it('should receive history visible cards', fakeAsync(() => {
    const testCard = makeTestCard();
    cardServiceMock.getHistoryVisibleCards.mockReturnValue(of([testCard]));
    component.ngAfterViewInit();
    tick();
    expect(component.historyVisibleCards()).toEqual([testCard]);
  }));

  it('should handle history visible cards error', fakeAsync(() => {
    cardServiceMock.getHistoryVisibleCards.mockReturnValue(throwError(() => new Error('Test error')));
    component.ngAfterViewInit();
    tick();
    expect(component.notificationStatus().message).toBe('Error fetching data');
    expect(component.notificationStatus().type).toBe(NotificationTypeValues.Error);
  }));

  it('should update filter', () => {
    component.onFilterChanged('test');
    expect(component.filter()).toBe('test');
  });

  it('should filter cards based on filter', () => {
    const testCardAlpha = makeTestCard('testAlpha');
    const testCardBravo = makeTestCard('testBravo');
    const testCardCharlie = makeTestCard('testCharlie');
    component.latestVisibleCards.set([testCardAlpha, testCardBravo]);
    component.historyVisibleCards.set([testCardBravo, testCardCharlie]);
    component.filter.set('Bravo');
    expect(component.latestVisibleCardsFiltered()).toEqual([testCardBravo]);
    expect(component.historyVisibleCardsFiltered()).toEqual([testCardBravo]);
  });

  it('should filter cards with empty filter', fakeAsync(() => {
    const testCardAlpha = makeTestCard('testAlpha');
    const testCardBravo = makeTestCard('testBravo');
    const testCardCharlie = makeTestCard('testCharlie');
    component.latestVisibleCards.set([testCardAlpha, testCardBravo]);
    component.historyVisibleCards.set([testCardBravo, testCardCharlie]);
    component.filter.set('');
    expect(component.latestVisibleCardsFiltered()).toEqual([testCardAlpha, testCardBravo]);
    expect(component.historyVisibleCardsFiltered()).toEqual([testCardBravo, testCardCharlie]);
  }));

  it('should update notification status correctly: no services', fakeAsync(() => {
    cardServiceMock.getLatestVisibleCards.mockReturnValue(of([]));
    component.ngAfterViewInit();
    tick();
    expect(component.notificationStatus().message).toBe('Services:  / 0 Total');
    expect(component.notificationStatus().type).toBe(NotificationTypeValues.Success);
  }));

  it('should update notification status correctly: many', fakeAsync(() => {
    cardServiceMock.getLatestVisibleCards.mockReturnValue(
      of([
        makeTestCard('R1', 'RED(1)'),
        makeTestCard('A1', 'AMBER(1)'),
        makeTestCard('A2', 'AMBER(2)'),
        makeTestCard('G1', 'GREEN(1)'),
        makeTestCard('G2', 'GREEN(2)'),
        makeTestCard('G3', 'GREEN(3)'),
        makeTestCard('B1', 'BLACK(Not Monitored)'),
        makeTestCard('B2', 'BLACK(Not Monitored)'),
        makeTestCard('B3', 'BLACK(Not Monitored)'),
        makeTestCard('B4', 'BLACK(Not Monitored)'),
      ])
    );
    component.ngAfterViewInit();
    tick();
    expect(component.notificationStatus().message).toBe('Services: 1 Red | 2 Amber | 3 Green | 4 Black / 10 Total');
    expect(component.notificationStatus().type).toBe(NotificationTypeValues.Warning);
  }));
});
