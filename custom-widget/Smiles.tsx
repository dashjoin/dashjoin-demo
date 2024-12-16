import { Icon } from "@mui/material"
import { Widget } from "../model/widget"
import { expression, text, title } from "../api/Const"
import { useEffect, useRef } from "react"
import { useExpression } from "../hooks/useExpression"
import { PrintError } from "../components/PrintError"
import { Loading } from "react-admin"
const SmilesDrawer = require('smiles-drawer')

export const Smiles = ({ widget }: { widget: Widget }) => {

    const imgRef = useRef<HTMLImageElement>(null);
    const drawer = new SmilesDrawer.SmiDrawer();

    const { data, isLoading, error } = useExpression(false, widget.text)
    
    useEffect(() => {
        drawer.draw(data ? data : 'C', imgRef.current, 'light');
    });
    
    if (isLoading) return <Loading />
    if (error) return <PrintError error={error}></PrintError>

    return (
        <div>
            <img ref={imgRef} width={300}></img>
        </div>
    );
}

export const config = {
    id: 'smiles',
    title: 'Smiles',
    description: 'Renders a SMILES string as a 3D molecule',
    version: 1,
    icon: <Icon>science</Icon>,
    controls: {
        type: 'autoform',
        schema: {
            properties: {
                title: title,
                text: expression
            }
        }
    }
}