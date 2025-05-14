import { ServiceCard } from './models/service-card';
import { RawStatus } from './models/raw-status';
import { ServiceConfiguration } from './models/service-configuration';
import { RefinedStatus } from './models/refined-status';

const TEST_NAME = 'TestSvc';
const TEST_RAW_STATUS_STR = 'GREEN(AM_OK)';
const TEST_FIRST_SEEN = '1989-05-29T10:00:00Z';
const TEST_LAST_SEEN = '1989-05-29T12:00:00Z';

export function makeTestCard(
  name = TEST_NAME,
  rawStatusStr = TEST_RAW_STATUS_STR,
  firstSeen = TEST_FIRST_SEEN,
  lastSeen = TEST_LAST_SEEN
): ServiceCard {
  return {
    id: `test_${name}_${firstSeen}`,
    configuration: makeTestConfiguration(name),
    status: makeTestStatus(name, rawStatusStr, firstSeen, lastSeen),
    derived: {
      icon: 'toast-test-data',
      timeFrame: 'TEST-00-00 00:00 UTC - TEST-00-00 00:00 UTC [_ days __ hours __ minutes]',
    },
  } as ServiceCard;
}

export function makeTestConfiguration(name = TEST_NAME): ServiceConfiguration {
  return {
    name: name,
    env: 'TestEnv',
    snowID: '12345',
    description: 'TestDescription',
    maintenanceMessage: 'ConfigurationTestMessage',
    links: {
      home: '127.0.0.1/home',
      snow: '127.0.0.1/snow',
      support: '127.0.0.1/support',
      documentation: '127.0.0.1/documentation',
      github: '127.0.0.1/github',
    },
  } as ServiceConfiguration;
}

export function makeTestStatus(
  name = TEST_NAME,
  rawStatusStr = TEST_RAW_STATUS_STR,
  firstSeen = TEST_FIRST_SEEN,
  lastSeen = TEST_LAST_SEEN
): RefinedStatus {
  return {
    serviceName: name,
    env: 'TestEnv',
    status: new RawStatus(rawStatusStr),
    maintenanceMessage: 'StatusTestMessage',
    firstSeen: firstSeen,
    lastSeen: lastSeen,
  } as RefinedStatus;
}
