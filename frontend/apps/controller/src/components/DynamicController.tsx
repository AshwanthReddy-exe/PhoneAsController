import DPadLayout from '../layouts/DPadLayout';
import TwoButtonLayout from '../layouts/TwoButtonLayout';
import ABCDLayout from '../layouts/ABCDLayout';

interface DynamicControllerProps {
  gameType: string;
  onInput: (action: number) => void;
}

export default function DynamicController({ gameType, onInput }: DynamicControllerProps) {
  switch (gameType) {
    case 'SNAKE':
      return <DPadLayout onInput={onInput} />;
    case 'PONG':
      return <TwoButtonLayout onInput={onInput} />;
    case 'TRIVIA':
      return <ABCDLayout onInput={onInput} />;
    default:
      return <DPadLayout onInput={onInput} />;
  }
}
