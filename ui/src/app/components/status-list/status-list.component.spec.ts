import { CommonModule } from '@angular/common';
import { ComponentRef, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { CpsDividerComponent, CpsIconComponent, CpsInputComponent, CpsProgressCircularComponent, CpsTooltipDirective } from 'cps-ui-kit';
import { MockComponents, MockDirectives, MockModule, ngMocks } from 'ng-mocks';
import { SharedModule } from '../../modules/shared/shared.module';
import { StatusListComponent } from './status-list.component';
import { makeTestCard } from '../../test.data';
import { NEVER } from 'rxjs';
import { CardService } from '../../services/card/card.service';
import { RefreshService } from '../../services/refresh/refresh.service';

describe('StatusListComponent', () => {
  let component: StatusListComponent;
  let fixture: ComponentFixture<StatusListComponent>;
  let componentRef: ComponentRef<StatusListComponent>;

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
      declarations: [StatusListComponent],
      imports: [
        CommonModule,
        ...MockComponents(CpsInputComponent, CpsDividerComponent, CpsIconComponent, CpsProgressCircularComponent),
        ...MockDirectives(CpsTooltipDirective),
        MockModule(SharedModule),
      ],
      providers: [
        { provide: RefreshService, useValue: refreshServiceMock },
        { provide: CardService, useValue: cardServiceMock },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(StatusListComponent);
    component = fixture.componentInstance;
    componentRef = fixture.componentRef;
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with empty cards', () => {
    expect(component.cards()).toEqual([]);
  });

  it('should update cards', () => {
    const testCards = [makeTestCard('TstSvc1'), makeTestCard('TstSvc2')];
    componentRef.setInput('cards', testCards);
    fixture.detectChanges();
    expect(component.cards()).toEqual(testCards);
  });

  it('should display cards', fakeAsync(() => {
    const testCards = [makeTestCard('TstSvc1'), makeTestCard('TstSvc2')];
    componentRef.setInput('cards', testCards);
    fixture.detectChanges();

    const serviceCards = ngMocks.findAll('.service-card');
    expect(serviceCards.length).toBe(testCards.length);
  }));
});
