export type RoomMap = {
    rooms: Room[];
}

export type Room = {
    name: string;
    category: string;
    position?: Position2D;
    dimensions?: Size2D;
    color?: string;
    bedOutsideRoom?: boolean;
    waitingRoom: boolean;
    numOccupants: number;
    innerWaitingRoom?: boolean;
}

export type Position2D = {
    x: number;
    y: number;
}

export type Size2D = {
    width: number;
    height: number;
}

export type Attributes = {
    attributes: Attribute[];
}

export type Attribute = {
    name: string;
    index: number;
}