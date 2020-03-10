import { EditorState, Transaction } from "prosemirror-state";
import { EditorView } from "prosemirror-view";
import { Schema, DOMParser } from "prosemirror-model";
import { marks, schema } from "prosemirror-schema-basic";
import { addListNodes } from "prosemirror-schema-list";
import {
  getBlocksFromDocument,
  createView
} from "@guardian/prosemirror-typerighter";
import { undo, redo } from "prosemirror-history";
import { undoInputRule } from "prosemirror-inputrules";
import { keymap } from "prosemirror-keymap";
import { history } from "prosemirror-history";
import {
  createTyperighterPlugin,
  createBoundCommands
} from "@guardian/prosemirror-typerighter";
import { IMatch } from "@guardian/prosemirror-typerighter/dist/interfaces/IMatch";

const appSchema = new Schema({
  nodes: addListNodes(schema.spec.nodes as any, "paragraph block*", "block"),
  marks
});

const parser = DOMParser.fromSchema(appSchema);

export const getBlocksFromHtmlString = (htmlStr: string) => {
  const element = document.createElement("div");
  element.innerHTML = htmlStr;
  const doc = parser.parse(element);
  return getBlocksFromDocument(doc, "id");
};

const createBasePlugins = () => [
  keymap(editorKeymap),
  history({ depth: 100, newGroupDelay: 500 })
];

export const createTyperighterEditorView = (
  editorEl: HTMLDivElement,
  sidebarEl: HTMLDivElement,
  htmlStr: string,
  matches: IMatch[]
): EditorView<typeof schema> => {
  const contentNode = document.createElement("div");
  contentNode.innerHTML = htmlStr;

  const { store, getState, plugin } = createTyperighterPlugin({
    matches
  });

  const editorView: EditorView = new EditorView(editorEl, {
    state: EditorState.create({
      doc: DOMParser.fromSchema(schema).parse(contentNode),
      plugins: [...createBasePlugins(), plugin]
    })
  });

  const sidebarMatchesEl = document.createElement("div");
  sidebarMatchesEl.classList.add("mt-2")
  const sidebarControlsEl = document.createElement("div");
  sidebarEl.appendChild(sidebarControlsEl);
  sidebarEl.appendChild(sidebarMatchesEl);
  const commands = createBoundCommands(editorView, getState);

  createView(
    editorView,
    store,
    {} as any,
    commands,
    sidebarMatchesEl,
    sidebarControlsEl,
    "mailto:example@typerighter.co.uk"
  );

  return editorView;
};

const createAddHardBreak = (schema: Schema) => (
  state: EditorState,
  dispatch?: (tr: Transaction) => void
) => {
  if (dispatch) {
    dispatch(
      state.tr
        .replaceSelectionWith(schema.nodes.hard_break.create())
        .scrollIntoView()
    );
  }
  return true;
};

export const editorKeymap = {
  "Mod-z": undo,
  "Shift-Mod-z": redo,
  Backspace: undoInputRule,
  Enter: createAddHardBreak(schema)
};
