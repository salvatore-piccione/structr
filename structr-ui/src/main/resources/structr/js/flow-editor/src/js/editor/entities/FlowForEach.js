'use strict';

import {FlowNode} from "./FlowNode.js";
import {FlowSockets} from "../FlowSockets.js";

export class FlowForEach extends FlowNode {

    constructor(node) {
        super(node);
    }

    getComponent() {
        let scopedDbNode = this.dbNode;
        return new D3NE.Component('Loop', {
            template: FlowForEach._nodeTemplate(),
            builder(node) {
                let socket = FlowSockets.getInst();
                let prev = new D3NE.Input('Prev', socket.getSocket('prev'), true);
                let next = new D3NE.Output('Next', socket.getSocket('next'));
                let dataSource = new D3NE.Input('DataSource', socket.getSocket('dataSource'));
                let dataTarget = new D3NE.Output('DataTarget', socket.getSocket('dataTarget'));
                let forEachBody = new D3NE.Output('FirstAction', socket.getSocket('loopBody'));

                if (scopedDbNode !== undefined && scopedDbNode.isStartNodeOfContainer !== undefined && scopedDbNode.isStartNodeOfContainer !== null) {
                    node.isStartNode = true;
                } else {
                    node.isStartNode = false;
                }

                node.data.dbNode = scopedDbNode;

                return node
                    .addInput(prev)
                    .addInput(dataSource)
                    .addOutput(next)
                    .addOutput(forEachBody)
                    .addOutput(dataTarget);
            },
            worker(node, inputs, outputs) {
            }
        });
    }

    static _nodeTemplate() {
        return `
            <div class="title {{node.isStartNode ? 'startNode' : ''}}">{{node.title}}</div>
                <content>
                    <column al-if="node.controls.length&gt;0 || node.inputs.length&gt;0">
                        <!-- Inputs-->
                        <div al-repeat="input in node.inputs" style="text-align: left">
                            <div class="socket input {{input.socket.id}} {{input.multipleConnections?'multiple':''}} {{input.connections.length&gt;0?'used':''}}" al-pick-input="al-pick-input" title="{{input.socket.name}}
                {{input.socket.hint}}"></div>
                            <div class="input-title" al-if="!input.showControl()">{{input.title}}</div>
                            <div class="input-control" al-if="input.showControl()" al-control="input.control"></div>
                        </div>
                        <!-- Controls-->
                        <div class="control" al-repeat="control in node.controls" style="text-align: center" :width="control.parent.width - 2 * control.margin" :height="control.height" al-control="control"></div>
                    </column>
                    <column>
                        <!-- Outputs-->
                        <div al-repeat="output in node.outputs" style="text-align: right">
                            <div class="output-title">{{output.title}}</div>
                            <div class="socket output {{output.socket.id}} {{output.connections.length>0?'used':''}}" al-pick-output="al-pick-output" title="{{output.socket.name}}
                {{output.socket.hint}}"></div>
                        </div>
                    </column>
                </content>
            </div>
        `;
    }

}