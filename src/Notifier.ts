import { Emitter, Hook } from "untrue";

import Detonator, { Platform } from "detonator";

export interface Message<K extends Record<string, string>> {
  messageId: string;
  senderId: string;
  data: K;
}

export type NotifierModuleSignatures = {
  token: (token: string | null) => any;
  message: (message: Message<Record<string, any>>) => any;
};

class NotifierModule extends Emitter<NotifierModuleSignatures> {
  private token: string | null = null;

  constructor() {
    super();

    Detonator.emitter.on(
      "com.iconshot.detonator.notifier.token",
      (token: string) => {
        this.token = token.length > 0 ? token : null;

        this.emit("token", this.token);
      }
    );

    Detonator.emitter.on(
      "com.iconshot.detonator.notifier.message",
      (data: string) => {
        const message: Message<any> = JSON.parse(data);

        this.emit("message", message);
      }
    );
  }

  public async checkPermission(): Promise<boolean> {
    if (Platform.get() !== "ios") {
      return true;
    }

    return await Detonator.request(
      "com.iconshot.detonator.notifier::checkPermission"
    ).fetchAndDecode();
  }

  public async requestPermission(): Promise<boolean> {
    if (Platform.get() !== "ios") {
      return true;
    }

    return await Detonator.request(
      "com.iconshot.detonator.notifier::requestPermission"
    ).fetchAndDecode();
  }

  public async registerForRemoteMessages(): Promise<void> {
    if (Platform.get() !== "ios") {
      return;
    }

    await Detonator.request(
      "com.iconshot.detonator.notifier::registerForRemoteMessages"
    ).fetch();
  }

  public useToken(): string | null {
    const update = Hook.useUpdate();

    Hook.useMountEffect((): (() => void) => {
      this.on("token", update);

      return (): void => {
        this.off("token", update);
      };
    });

    return this.token;
  }

  public async showNotification(data: {
    id: number;
    title: string;
    body?: string | null;
    pictureUrl?: string | null;
    android?: {
      channelId: string;
      priority?: "high" | "default" | "low" | "max" | "min" | null;
    } | null;
    ios?: {} | null;
  }): Promise<void> {
    let { android, ios, ...tmpData } = data;

    switch (Platform.get()) {
      case "android": {
        if (android === undefined || android === null) {
          throw new Error("Additional data is required for Android platform.");
        }

        tmpData = { ...tmpData, ...android };

        break;
      }

      case "ios": {
        tmpData = { ...tmpData, ...ios };

        break;
      }
    }

    await Detonator.request(
      "com.iconshot.detonator.notifier::showNotification",
      tmpData
    ).fetch();
  }

  public async createChannel(data: {
    id: string;
    name: string;
    description?: string | null;
    importance?:
      | "high"
      | "default"
      | "low"
      | "max"
      | "min"
      | "none"
      | "unspecified"
      | null;
    groupId?: string | null;
  }): Promise<boolean> {
    if (Platform.get() !== "android") {
      return false;
    }

    return await Detonator.request(
      "com.iconshot.detonator.notifier::createChannel",
      data
    ).fetchAndDecode();
  }

  public async createChannelGroup(data: {
    id: string;
    name: string;
    description?: string | null;
  }): Promise<boolean> {
    if (Platform.get() !== "android") {
      return false;
    }

    return await Detonator.request(
      "com.iconshot.detonator.notifier::createChannelGroup",
      data
    ).fetchAndDecode();
  }
}

export const Notifier = new NotifierModule();
