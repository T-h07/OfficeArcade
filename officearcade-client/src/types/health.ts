export type HealthResponse = {
  status: string;
  application: string;
  profile: string;
  timestamp: string;
};

export type ServerConnectionStatus = "loading" | "online" | "offline";
