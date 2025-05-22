import { Utils } from './utils';
import { TestBed } from '@angular/core/testing';
import { RawStatusColor } from '../../models/raw-status';
import { RefinedStatus } from '../../models/refined-status';
import { makeTestCard } from '../../test.data';

describe('UtilsService', () => {
  let utils: Utils;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [Utils],
    });
    utils = TestBed.inject(Utils);
  });

  it('getIcon should get toast-error for Red', () => {
    expect(utils.getIcon(RawStatusColor.RED)).toBe('toast-error');
  });

  it('getIcon should get toast-warning for Amber', () => {
    expect(utils.getIcon(RawStatusColor.AMBER)).toBe('toast-warning');
  });

  it('getIcon should get toast-success for Green', () => {
    expect(utils.getIcon(RawStatusColor.GREEN)).toBe('toast-success');
  });

  it('getIcon should get toast-info for Black', () => {
    expect(utils.getIcon(RawStatusColor.BLACK)).toBe('toast-info');
  });

  it('getDurationString should properly get durationString', () => {
    const testStatus: RefinedStatus = {
      firstSeen: '1989-05-20T10:00:00Z',
      lastSeen: '1989-05-29T11:23:00Z',
    } as unknown as RefinedStatus;
    expect(utils.getTimeFrame(testStatus)).toBe('1989-05-20 10:00 UTC - 1989-05-29 11:23 UTC [9 days 1 hour 23 minutes]');
  });

  it('getTimeFrame should properly get time frame', () => {
    expect(utils.getDurationString(new Date('1989-05-20T10:00:00Z'), new Date('1989-05-29T11:23:00Z'))).toBe('9 days 1 hour 23 minutes');
  });

  it('getTimeFrameToolTip should properly get time frame tooltip', () => {
    const testStatus: RefinedStatus = {
      firstSeen: '1989-05-20T10:00:00Z',
      lastSeen: '1989-05-29T11:23:00Z',
    } as unknown as RefinedStatus;
    expect(utils.getTimeFrameToolTip(testStatus)).toBe(
      'First seen in this state: 1989-05-20 10:00 UTC <br>Last seen in this state: 1989-05-29 11:23 UTC<br>Observed in this state for: 9 days 1 hour 23 minutes'
    );
  });

  it('applyFilter should filter properly', () => {
    const testCardEnvName = makeTestCard('TstSvc1 findME');
    testCardEnvName.configuration.env = 'HITme';

    const testCardStatus = makeTestCard('TstSvc2', 'GREEN(fINDmE and hitMe)');

    const testCardDescMsg = makeTestCard('TstSvc3');
    testCardDescMsg.configuration.description = 'HitMe';
    testCardDescMsg.status.maintenanceMessage = 'FindMe';

    const testCannotFind = makeTestCard('TstSvc4');

    const filterTerm = 'HitMe FindMe';
    const testCards = [testCardEnvName, testCardStatus, testCardDescMsg, testCannotFind];
    const expectedCards = [testCardEnvName, testCardStatus, testCardDescMsg];

    expect(utils.applyFilter(filterTerm, testCards)).toEqual(expectedCards);
  });
});
