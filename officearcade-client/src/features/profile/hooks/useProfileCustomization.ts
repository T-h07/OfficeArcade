import { useCallback, useEffect, useMemo, useState } from "react";
import { equipProfileItem, getMyProfile, ProfileApiError, unequipProfileItem } from "../api/profileApi";
import type {
  ProfileMeResponse,
  ProfileOwnedCosmetic
} from "../types/profile.types";
import { PROFILE_CUSTOMIZATION_CATEGORY_ORDER } from "../types/profile.types";

type CustomizationCategory = (typeof PROFILE_CUSTOMIZATION_CATEGORY_ORDER)[number];

type UseProfileCustomizationResult = {
  profile: ProfileMeResponse | null;
  selectedCategory: CustomizationCategory;
  itemsInSelectedCategory: ProfileOwnedCosmetic[];
  isLoading: boolean;
  isMutating: boolean;
  errorMessage: string | null;
  actionMessage: string | null;
  setSelectedCategory: (category: CustomizationCategory) => void;
  refresh: () => Promise<void>;
  equip: (itemId: string) => Promise<boolean>;
  unequip: (itemId: string) => Promise<boolean>;
  clearActionMessage: () => void;
};

function resolveErrorMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }
  return fallbackMessage;
}

export function useProfileCustomization(
  accessToken: string | null,
  onUnauthorized: () => void
): UseProfileCustomizationResult {
  const [profile, setProfile] = useState<ProfileMeResponse | null>(null);
  const [selectedCategory, setSelectedCategory] = useState<CustomizationCategory>("OUTFIT");
  const [isLoading, setIsLoading] = useState(true);
  const [isMutating, setIsMutating] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!accessToken) {
      setProfile(null);
      setErrorMessage(null);
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setErrorMessage(null);

    try {
      const nextProfile = await getMyProfile(accessToken);
      setProfile(nextProfile);
    } catch (error) {
      if (error instanceof ProfileApiError && error.status === 401) {
        onUnauthorized();
        return;
      }
      setErrorMessage(resolveErrorMessage(error, "Unable to load profile customization data."));
    } finally {
      setIsLoading(false);
    }
  }, [accessToken, onUnauthorized]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const itemsInSelectedCategory = useMemo(() => {
    if (!profile) {
      return [];
    }
    return profile.ownedCosmetics.filter((item) => item.category === selectedCategory);
  }, [profile, selectedCategory]);

  async function runMutation(
    action: () => Promise<void>,
    successMessage: string,
    fallbackErrorMessage: string
  ): Promise<boolean> {
    if (!accessToken) {
      return false;
    }

    setIsMutating(true);
    setErrorMessage(null);
    setActionMessage(null);

    try {
      await action();
      setActionMessage(successMessage);
      await refresh();
      return true;
    } catch (error) {
      if (error instanceof ProfileApiError && error.status === 401) {
        onUnauthorized();
        return false;
      }
      setErrorMessage(resolveErrorMessage(error, fallbackErrorMessage));
      return false;
    } finally {
      setIsMutating(false);
    }
  }

  return {
    profile,
    selectedCategory,
    itemsInSelectedCategory,
    isLoading,
    isMutating,
    errorMessage,
    actionMessage,
    setSelectedCategory,
    refresh,
    equip: (itemId) =>
      runMutation(
        () => equipProfileItem(accessToken!, itemId),
        "Item equipped.",
        "Unable to equip selected item."
      ),
    unequip: (itemId) =>
      runMutation(
        () => unequipProfileItem(accessToken!, itemId),
        "Item unequipped.",
        "Unable to unequip selected item."
      ),
    clearActionMessage: () => setActionMessage(null)
  };
}
