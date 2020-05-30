import { EditorState } from "prosemirror-state";
import { EditorView } from "prosemirror-view";
import { Schema, DOMParser } from "prosemirror-model";
import { marks, schema } from "prosemirror-schema-basic";
import { addListNodes } from "prosemirror-schema-list";
import { getBlocksFromDocument } from "@guardian/prosemirror-typerighter";

const appSchema = new Schema({
  nodes: addListNodes(schema.spec.nodes as any, "paragraph block*", "block"),
  marks
});

const parser = DOMParser.fromSchema(appSchema);

export const getBlocksFromHtmlString = (htmlStr: string) => {
  const element = document.createElement("div");
  element.innerHTML = htmlStr;
  const doc = parser.parse(element);
  return getBlocksFromDocument(doc);
};
