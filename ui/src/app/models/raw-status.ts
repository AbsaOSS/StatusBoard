export enum RawStatusColor {
  RED = 'RED',
  AMBER = 'AMBER',
  GREEN = 'GREEN',
  BLACK = 'BLACK',
}

export class RawStatus {
  color: RawStatusColor;
  statusMessage: string;
  intermittent: boolean;

  constructor(fullStatusMessage: string) {
    // Initialize with error values
    this.color = RawStatusColor.RED;
    this.statusMessage = `FATAL: Error while parsing raw status: ${fullStatusMessage}`;
    this.intermittent = false;
    // Attempt to obtain actual values
    (Object.values(RawStatusColor) as RawStatusColor[]).forEach(color => {
      if (fullStatusMessage.startsWith(color)) {
        this.color = color;
        this.statusMessage = fullStatusMessage.substring(color.length + 1, fullStatusMessage.length - 1);
        this.intermittent = fullStatusMessage[color.length] == '['; // Intermittent is in square brackets
      }
    });
  }

  toString(): string {
    return this.intermittent ? `${this.color}[${this.statusMessage}]` : `${this.color}(${this.statusMessage})`;
  }
}
