/**
 * Readytogo Cloud Functions.
 *
 * 함수마다 maxInstances를 따로 준다. 전역 기본값은 새 함수를 추가할 때 실수로 크게
 * 열리지 않도록 낮게 둔다.
 */
import {initializeApp} from "firebase-admin/app";
import {setGlobalOptions} from "firebase-functions";

initializeApp();
setGlobalOptions({maxInstances: 2});

export {editDay} from "./editDay";
export {reviewIssues} from "./reviewIssues";
