import axios from 'axios';

const API_BASE_URL = '/api';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor to add JWT token if available
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('airconsole_token');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export interface RoomDTO {
  roomId: string;
  roomCode: string;
  hostId: string;
  gameType: string;
  maxPlayers: number;
  status: string;
}

export interface PlayerDTO {
  playerId: string;
  token: string;
  role: string;
}

export const createRoomApi = async (hostId: string, gameType: string, maxPlayers: number) => {
  const response = await apiClient.post<RoomDTO>('/rooms', { hostId, gameType, maxPlayers });
  return response.data;
};

export const joinRoomApi = async (roomCode: string, playerName: string) => {
  // First, validate/join the room to get the actual UUID
  const roomResponse = await apiClient.post<RoomDTO>('/rooms/join', { roomCode });
  const roomId = roomResponse.data.roomId;
  
  // Second, register the player in the player-service
  const playerResponse = await apiClient.post<PlayerDTO>('/players/register', { roomId, playerName });
  return { player: playerResponse.data, roomId };
};

export const sendInputApi = async (playerId: string, roomId: string, action: number, tickNumber: number = 0) => {
  await apiClient.post('/games/input', { playerId, roomId, action, tickNumber });
};

export const startGameApi = async (roomId: string, gameType: string, playerIds: string[]) => {
  const response = await apiClient.post(`/games/start?roomId=${roomId}&gameType=${gameType}`, playerIds);
  return response.data;
};

export const getPlayersApi = async (roomId: string) => {
  const response = await apiClient.get(`/players/${roomId}/all`);
  return response.data;
};

export const getGameLayoutApi = async (gameType: string) => {
  const response = await apiClient.get(`/games/${gameType}/layout`);
  return response.data;
};
