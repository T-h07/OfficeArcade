import { getGameTypeVisual } from "./gameTypeVisuals";

type GameTypeBadgeProps = {
  gameTypeCode: string;
  displayName?: string;
  className?: string;
};

export function GameTypeBadge({ gameTypeCode, displayName, className = "" }: GameTypeBadgeProps) {
  const visual = getGameTypeVisual(gameTypeCode, displayName);

  return (
    <span className={`${visual.badgeClassName} ${className}`.trim()} data-icon={visual.iconText}>
      {visual.title}
    </span>
  );
}
