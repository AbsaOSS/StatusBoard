import { ServiceConfiguration } from './service-configuration';
import { RefinedStatus } from './refined-status';

export interface HierarchicalServiceCardWithHistory extends HierarchicalServiceCard, ServiceCardWithHistory {}

export interface HierarchicalServiceCard extends ServiceCard {
  depth: number;
}

export interface ServiceCardWithHistory extends ServiceCard {
  history: ServiceCard[];
}

export interface ServiceCard {
  id: string;
  configuration: ServiceConfiguration;
  status: RefinedStatus;
  derived: {
    icon: string;
    cssColor: string;
    timeFrame: string;
  };
}
