type RewardTabOption<T extends string> = {
  value: T;
  label: string;
  count?: number;
};

type RewardCategoryTabsProps<T extends string> = {
  label: string;
  options: Array<RewardTabOption<T>>;
  selected: T;
  onSelect: (value: T) => void;
  disabled?: boolean;
};

export function RewardCategoryTabs<T extends string>({
  label,
  options,
  selected,
  onSelect,
  disabled = false
}: RewardCategoryTabsProps<T>) {
  return (
    <div className="space-y-2">
      <p className="text-xs uppercase tracking-[0.16em] text-oa-muted">{label}</p>
      <div className="oa-reward-tabs" role="tablist" aria-label={label}>
        {options.map((option) => {
          const isActive = option.value === selected;
          return (
            <button
              key={option.value}
              type="button"
              onClick={() => onSelect(option.value)}
              className={`oa-reward-tab ${isActive ? "oa-reward-tab-active" : ""}`}
              disabled={disabled}
              role="tab"
              aria-selected={isActive}
            >
              <span>{option.label}</span>
              {option.count !== undefined ? <span className="text-[11px] text-oa-muted">{option.count}</span> : null}
            </button>
          );
        })}
      </div>
    </div>
  );
}
