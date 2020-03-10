import * as React from "react";
import { useEffect, useRef } from "react";

import { createTyperighterEditorView } from "utils/prosemirror";
import { IMatch } from "@guardian/prosemirror-typerighter/dist/interfaces/IMatch";

interface IProps {
  htmlStr: string;
  matches: IMatch[];
}

const ProsemirrorDocument = ({ htmlStr, matches }: IProps) => {
  const editorEl = useRef<HTMLDivElement>(null);
  const sidebarEl = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!editorEl.current || !sidebarEl.current) {
      return;
    }

    // At the moment, we create a new editor instance every time we receive
    // a new document. If necessary, we can be more efficient by keeping the
    // same view every time, and updating the view and plugin state with
    // view.updateState().
    sidebarEl.current.innerHTML = "";
    const view = createTyperighterEditorView(
      editorEl.current,
      sidebarEl.current,
      htmlStr,
      matches
    );
    return () => view.destroy();
  }, [htmlStr]);

  return (
    <div className="row">
      <div className="col-9 position-relative">
        <div
          id="editor"
          className="ProseMirror-example-setup-style"
          ref={editorEl}
          data-testid="edit-form-rich-text"
        />
      </div>
      <div className="col-3">
        <div ref={sidebarEl} />
      </div>
    </div>
  );
};

export default ProsemirrorDocument;
