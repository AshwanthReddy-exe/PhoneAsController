import { useCallback, useRef } from 'react';
import { sendInputApi } from '@airconsole/shared';

export function useControllerInput(playerId: string, roomId: string) {
  const lastSendRef = useRef<number>(0);

  const sendInput = useCallback(
    (action: number) => {
      const now = Date.now();
      // 16ms throttle to prevent excessive dispatch
      if (now - lastSendRef.current < 16) return;
      lastSendRef.current = now;
      sendInputApi(playerId, roomId, action, 0).catch((err) => {
        console.error('Input dispatch failed', err);
      });
    },
    [playerId, roomId]
  );

  return { sendInput };
}
