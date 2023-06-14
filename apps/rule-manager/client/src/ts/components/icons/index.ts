import { appendIconComponentCache } from "@elastic/eui/src/components/icon/icon";
import { icon as arrowDown } from "@elastic/eui/src/components/icon/assets/arrow_down";
import { icon as arrowRight } from "@elastic/eui/src/components/icon/assets/arrow_right";
import { icon as arrowLeft } from "@elastic/eui/src/components/icon/assets/arrow_left";
import { icon as arrowStart } from "@elastic/eui/src/components/icon/assets/arrowStart";
import { icon as arrowEnd } from "@elastic/eui/src/components/icon/assets/arrowEnd";
import { icon as check } from "@elastic/eui/src/components/icon/assets/check";
import { icon as cross } from "@elastic/eui/src/components/icon/assets/cross";
import { icon as empty } from "@elastic/eui/src/components/icon/assets/empty";
import { icon as search } from "./search";
import { icon as error } from "@elastic/eui/src/components/icon/assets/error";
import { icon as exit } from "@elastic/eui/src/components/icon/assets/exit";
import { icon as starEmptySpace } from "@elastic/eui/src/components/icon/assets/star_empty";
import { icon as starFilled } from "@elastic/eui/src/components/icon/assets/star_filled";
import { icon as pencil } from "@elastic/eui/src/components/icon/assets/pencil";
import { icon as returnKey } from "@elastic/eui/src/components/icon/assets/return_key";
import { icon as warning } from "@elastic/eui/src/components/icon/assets/warning";
import { icon as pageSelect } from "@elastic/eui/src/components/icon/assets/pageSelect";

const cachedIcons = {
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
    warning,
    pageSelect
};

appendIconComponentCache(cachedIcons);
