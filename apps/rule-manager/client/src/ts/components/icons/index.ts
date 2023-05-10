import { ICON_TYPES } from "@elastic/eui";
import { appendIconComponentCache } from "@elastic/eui/es/components/icon/icon";
import { ValuesType } from "utility-types";
import { icon as arrowDown } from "@elastic/eui/es/components/icon/assets/arrow_down";
import { icon as arrowRight } from "@elastic/eui/es/components/icon/assets/arrow_right";
import { icon as arrowLeft } from "@elastic/eui/es/components/icon/assets/arrow_left";
import { icon as arrowStart } from "@elastic/eui/es/components/icon/assets/arrowStart";
import { icon as arrowEnd } from "@elastic/eui/es/components/icon/assets/arrowEnd";
import { icon as check } from "@elastic/eui/es/components/icon/assets/check";
import { icon as cross } from "@elastic/eui/es/components/icon/assets/cross";
import { icon as empty } from "@elastic/eui/es/components/icon/assets/empty";
import { icon as search } from "./search";
import { icon as error } from "@elastic/eui/es/components/icon/assets/error";
import { icon as exit } from "@elastic/eui/es/components/icon/assets/exit";
import { icon as starEmptySpace } from "@elastic/eui/es/components/icon/assets/star_empty";
import { icon as starFilled } from "@elastic/eui/es/components/icon/assets/star_filled";
import { icon as pencil } from "@elastic/eui/es/components/icon/assets/pencil";
import { icon as returnKey } from "@elastic/eui/es/components/icon/assets/return_key";
import { icon as warning } from "@elastic/eui/es/components/icon/assets/warning";

type IconComponentNameType = ValuesType<typeof ICON_TYPES>;
type IconComponentCacheType = Partial<Record<IconComponentNameType, unknown>>;

const cachedIcons: IconComponentCacheType = {
    arrowDown,
    arrowRight,
    arrowLeft,
    arrowStart,
    arrowEnd,
    search,
    empty,
    cross,
    check,
    error,
    exit,
    starEmptySpace,
    starFilled,
    pencil,
    returnKey,
    warning
};

appendIconComponentCache(cachedIcons);
