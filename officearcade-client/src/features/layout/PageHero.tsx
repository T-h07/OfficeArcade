import type { ReactNode } from "react";

type PageHeroProps = {
  kicker: string;
  title: string;
  subtitle?: string;
  tone?: "player" | "admin";
  rightSlot?: ReactNode;
  footerSlot?: ReactNode;
};

export function PageHero({ kicker, title, subtitle, tone = "player", rightSlot, footerSlot }: PageHeroProps) {
  return (
    <header className={`oa-hero ${tone === "admin" ? "oa-hero-admin" : ""}`}>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="oa-hero-kicker">{kicker}</p>
          <h1 className="oa-hero-title">{title}</h1>
          {subtitle ? <p className="oa-hero-subtitle">{subtitle}</p> : null}
        </div>
        {rightSlot ? <div className="flex items-center gap-2">{rightSlot}</div> : null}
      </div>
      {footerSlot ? <div className="mt-4 flex flex-wrap items-center gap-2">{footerSlot}</div> : null}
    </header>
  );
}
