import { EuiBasicTableColumn, EuiButton, EuiButtonEmpty, EuiFieldText, EuiFlexGroup, EuiFlexItem, EuiForm, EuiFormRow, EuiIcon, EuiInMemoryTable, EuiInMemoryTableProps, EuiInlineEditText, EuiLoadingSpinner, EuiModal, EuiModalBody, EuiModalHeader, EuiModalHeaderTitle, EuiSpacer, EuiText, EuiTextColor, EuiTitle, EuiToast, EuiToolTip } from "@elastic/eui"
import { css } from "@emotion/react"
import { Tag, useTags } from "./hooks/useTags";
import React, { useEffect, useState } from "react";
import styled from "@emotion/styled";
import { useCreateEditPermissions } from "./RulesTable";
import { RuleFormSection } from "./RuleFormSection";
import { ErrorIResponse } from "../utils/api";


type DeleteTagButtonProps = {
    editIsEnabled: boolean;
}

const DeleteTagButton = styled.button<DeleteTagButtonProps>(props => ({
    width: '16px',
    cursor: props.editIsEnabled ? 'pointer' : 'not-allowed',
}))

const editableNameWidth = '18rem';

const getTagMethodDisabledMessage = (methodName: string) => 
    `You do not have the correct permissions to ${methodName} a tag. Please contact Central Production if you need to do so.`

const DeleteTag = (
    {editIsEnabled, tag, openDeleteTagDialogue, isLoading, tagToDelete}: 
    {editIsEnabled: boolean, tag: Tag, openDeleteTagDialogue: (tag: Tag) => void, isLoading: boolean, tagToDelete: Tag | null}
) => 
    <EuiToolTip
        content={editIsEnabled ? "" : getTagMethodDisabledMessage('delete')}>
        <DeleteTagButton editIsEnabled={editIsEnabled} onClick={() => editIsEnabled ? openDeleteTagDialogue(tag) : null}>
            {isLoading && tagToDelete && tagToDelete.id === tag.id ? <EuiLoadingSpinner /> : <EuiIcon type="trash" color="danger"/>}
        </DeleteTagButton>
    </EuiToolTip>

export const EditableNameField = ({
    editIsEnabled,
    editTag,
    tag,
  }: { editIsEnabled: boolean, editTag: (tag: Tag) => void, tag: Tag, fetchTags: () => void }) => {
    const [tagName, setTagName] = useState(tag.name);
    useEffect(() => {
        setTagName(tag.name)
    }, [tag])
    return (
      <>
        <EuiFlexGroup css={css`width: ${editableNameWidth};`}>
            {editIsEnabled ? 
                <EuiInlineEditText
                    inputAriaLabel="Edit text inline"
                    defaultValue={tagName}
                    size='s'
                    onSave={(newInlineEditValue: string) => {
                        const newTag = {id: tag.id, name: newInlineEditValue}
                        editTag(newTag)
                    }}
                    css={css`width: 100%; color: black`}
                /> : <EuiToolTip
                        content={editIsEnabled ? "" : getTagMethodDisabledMessage('edit')}>
                        <EuiFlexItem>{tagName}</EuiFlexItem>
                    </EuiToolTip>
            }
        </EuiFlexGroup>
      </>
    );
  };

const createTagTableColumns = (
    editTag: (tag: Tag) => void, 
    fetchTags: () => void, 
    isLoading: boolean, 
    openDeleteTagDialogue: (tag: Tag) => void, 
    hasEditPermissions: boolean,
    tagToDelete: Tag | null,
): Array<EuiBasicTableColumn<Tag>> => {
    return [
        {
            field: 'name',
            name: 'Name',
            width: editableNameWidth,
            sortable: ({ name }) => name.toLowerCase(),
            render: (tagName, item) => <EditableNameField editIsEnabled={hasEditPermissions} editTag={editTag} tag={item} fetchTags={fetchTags} key={item.id} />,
          },
        {
            field: 'ruleCount',
            width: '10rem',
            name: 'Associated rules',
            render: (value: number) => !!value ? value : 0,
            sortable: true
        },
        {
            name: 'Delete',
            width: '4rem',
            actions: [{
                name: 'Delete',
                enabled: () => hasEditPermissions,
                render: (item, enabled) => <DeleteTag editIsEnabled={enabled} tag={item} openDeleteTagDialogue={openDeleteTagDialogue} isLoading={isLoading} tagToDelete={tagToDelete}/>
            }]
        }
    ]
}

const CreateTagForm = ({createTag, enabled, isLoadingCreatedTag}: {createTag: (tagName: string) => Promise<void>, enabled: boolean, isLoadingCreatedTag: boolean}) => {
    const [tagName, setTagName] = useState('');
    const [clientSideValidationError, setClientSideValidationError] = useState<string | null>(null);
    const createTagIfSuitable = (tagName: string) => {
        if (!tagName){
            setClientSideValidationError("A tag must have a name.")
        } else {
            createTag(tagName)
            setTagName('');
        }
    }
    return <EuiFlexGroup css={css`margin-bottom: 1rem; width: 100%;`}>
        <RuleFormSection title="CREATE NEW TAG">
            <EuiSpacer size="s" />
            <EuiFlexGroup css={css`display: flex; gap: 0.8rem;`}>
                <EuiFlexItem grow={true}>
                    <EuiToolTip
                    content={enabled ? "" : getTagMethodDisabledMessage('create')}>
                        <EuiFieldText 
                            placeholder="Tag name..." 
                            value={tagName} 
                            onChange={(e) => {setClientSideValidationError(null); setTagName(e.target.value || "")}}
                            disabled={!enabled}
                        />
                    </EuiToolTip>
                </EuiFlexItem>
                <EuiFlexItem grow={false}>
                    <EuiToolTip
                    content={enabled ? "" : getTagMethodDisabledMessage('create')}>
                        <EuiButton onClick={() => createTagIfSuitable(tagName)} color={"primary"} fill disabled={!enabled || isLoadingCreatedTag}>
                            {isLoadingCreatedTag ? <EuiLoadingSpinner size="s" /> : "Create Tag"}
                        </EuiButton>
                    
                    </EuiToolTip>
                </EuiFlexItem>
            </EuiFlexGroup>
            <EuiFlexGroup >
                <EuiTextColor color="danger">
                    {clientSideValidationError ? <EuiText css={css`margin-top: 0.5rem;`}>{clientSideValidationError}</EuiText> : null}
                </EuiTextColor>
            </EuiFlexGroup>
        </RuleFormSection>
    </EuiFlexGroup>
}

const deleteTagWarningCss = css`
    position: absolute;
    bottom: 20px;
    right: 20px;
    background-color: white;
    padding: 20px;
    z-index: 1;
    box-shadow: 2px 2px 10px rgba(0,0,0,0.4)
`;

const DeleteTagWarning = (
    {tag, setTagToDelete, deleteTag, setHideDeletionModal}: 
    {tag: Tag, setTagToDelete: (tag: Tag | null) => void, deleteTag: (tag: Tag) => Promise<ErrorIResponse | undefined>, setHideDeletionModal: (bool: boolean) => void}
) => {
    const deleteAndClosePrompty = (tag: Tag) => { 
        setHideDeletionModal(true);
        deleteTag(tag).then(() => {
            setTagToDelete(null);
            setHideDeletionModal(false);
        })
        
    }
    return <EuiModal 
        onClose={() => {setTagToDelete(null)}} 
        initialFocus="[name=popswitch]"
        color="danger"
    >
        <EuiModalHeader>
            <EuiModalHeaderTitle>Confirm tag deletion</EuiModalHeaderTitle>
        </EuiModalHeader>
        <EuiModalBody>
            <EuiText>
                Are you sure you want to delete the tag '{tag.name}'?
                <br/>
                This action is <strong>permanent</strong> and will remove the tag from all associated rules.
            </EuiText>
            <EuiSpacer />
            <EuiFlexGroup justifyContent="flexEnd">
                <EuiFlexItem grow={false}>
                    <EuiButtonEmpty onClick={() => {setTagToDelete(null)}}>Cancel</EuiButtonEmpty>
                </EuiFlexItem>
                <EuiFlexItem grow={false}>
                    <EuiButton onClick={() => deleteAndClosePrompty(tag)} color="danger">Delete the '{tag.name}' tag</EuiButton>
                </EuiFlexItem>
            </EuiFlexGroup>
        </EuiModalBody>
    </EuiModal>
}

const ServerErrorNotification =  ({error}: {error: string}) => {
    return <EuiToast
        title="Server Error"
        color="danger"
        iconType="error"
        css={deleteTagWarningCss}
    >
        <p>{error}</p>
    </EuiToast>

}
export const TagsTable = () => {
    const {tags, fetchTags, isLoading, tagRuleCounts, updateTag, deleteTag, createTag, error, isLoadingCreatedTag} = useTags();
    const items = Object.values(tags)

    const [tagToDelete, setTagToDelete] = useState<Tag | null>(null)
    const [hideDeletionModal, setHideDeletionModal] = useState(false);

    const openDeleteTagDialogue = (tag: Tag) => {
        setTagToDelete(tag)
    }
    const hasEditPermissions = useCreateEditPermissions();

    const columns = createTagTableColumns(updateTag, fetchTags, isLoading, openDeleteTagDialogue, hasEditPermissions, tagToDelete)

    return (<>
        <EuiFlexGroup>
            <EuiFlexItem css={css`width: 32rem;`}>
                <EuiFlexGroup>
                    <EuiFlexItem grow={false} css={css`padding-bottom: 20px;`}>
                        <EuiTitle>
                            <h1>Tags</h1>
                        </EuiTitle>
                    </EuiFlexItem>
                </EuiFlexGroup>
                <EuiFlexGroup>
                    <CreateTagForm createTag={createTag} enabled={hasEditPermissions} isLoadingCreatedTag={isLoadingCreatedTag} />
                </EuiFlexGroup>
                <EuiFlexGroup>
                    {tagToDelete && !hideDeletionModal ? <DeleteTagWarning tag={tagToDelete} setTagToDelete={setTagToDelete} deleteTag={deleteTag} setHideDeletionModal={setHideDeletionModal}/> : null}
                    <EuiFlexItem>
                        <EuiInMemoryTable
                            columns={columns}
                            items={items}
                            sorting={{sort: {
                                field: 'name',
                                direction: 'asc',
                            }}}
                        >
                        </EuiInMemoryTable>
                    </EuiFlexItem>
                </EuiFlexGroup>
            </ EuiFlexItem>
            <EuiFlexItem/>
            {error ? <ServerErrorNotification error={error}/> : null}
        </EuiFlexGroup>
    </>)
}
