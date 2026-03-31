import './Overview.scss';

import { Badge, Container } from 'react-bootstrap';
import { useDependencyHealth } from '../services/HealthService';

import { AiOutlineLoading } from 'react-icons/ai';
import { BsPersonFill } from 'react-icons/bs';
import React, { useCallback, useEffect, useRef, useState } from 'react';

const NODES: Record<string, { label: string; x: number; y: number; type: 'service' | 'infra' | 'user'; statusKeys?: string[] }> = {
    // Row 1: User (top center)
    'user':                { label: 'User',                x: 50,  y: 8,   type: 'user' },
    // Row 2: Gateway (center) + Bestsellers (right) + Inventory (far right)
    'gateway':             { label: 'Gateway',             x: 50,  y: 23,  type: 'service' },
    'fashion-bestseller':  { label: 'Fashion Bestseller',  x: 75,  y: 11,  type: 'service' },
    'toys-bestseller':     { label: 'Toys Bestseller',     x: 75,  y: 23,  type: 'service' },
    'hot-deals':           { label: 'Hot Deals',           x: 75,  y: 35,  type: 'service' },
    'inventory':           { label: 'Inventory',           x: 95,  y: 23,  type: 'service' },
    // Row 3: Redis (left) + Checkout (center)
    'redis':               { label: 'Redis',               x: 20,  y: 47,  type: 'infra' },
    'checkout':            { label: 'Checkout',            x: 50,  y: 47,  type: 'service' },
    // Row 4: Broker (left) + Orders (center)
    'broker':              { label: 'ActiveMQ / Kafka',    x: 20,  y: 65,  type: 'infra', statusKeys: ['activemq', 'kafka'] },
    'orders':              { label: 'Orders',              x: 50,  y: 65,  type: 'service' },
    // Row 5: RabbitMQ (left) + Notification (center)
    'rabbitmq':            { label: 'RabbitMQ',            x: 20,  y: 83,  type: 'infra' },
    'notification':        { label: 'Notification',        x: 50,  y: 83,  type: 'service' },
};

interface Connection {
    from: string;
    to: string;
    style: 'solid' | 'dashed';
}

const CONNECTIONS: Connection[] = [
    { from: 'user',    to: 'gateway',            style: 'solid' },
    { from: 'gateway', to: 'fashion-bestseller', style: 'solid' },
    { from: 'gateway', to: 'toys-bestseller',    style: 'solid' },
    { from: 'gateway', to: 'hot-deals',          style: 'solid' },
    { from: 'gateway', to: 'checkout',           style: 'solid' },
    { from: 'fashion-bestseller', to: 'inventory', style: 'solid' },
    { from: 'toys-bestseller',    to: 'inventory', style: 'solid' },
    { from: 'hot-deals',          to: 'inventory', style: 'solid' },
    { from: 'checkout', to: 'broker',  style: 'dashed' },
    { from: 'checkout', to: 'redis',   style: 'solid' },
    { from: 'broker',   to: 'orders',  style: 'dashed' },
    { from: 'orders',   to: 'rabbitmq',     style: 'dashed' },
    { from: 'rabbitmq', to: 'notification', style: 'dashed' },
];

interface LineData {
    x1: number; y1: number; x2: number; y2: number;
    style: string;
    status: string;
}

const Overview: React.FC = () => {
    const { content, error, isLoading, timestamp } = useDependencyHealth();
    const graphRef = useRef<HTMLDivElement>(null);
    const nodeRefs = useRef<Record<string, HTMLDivElement | null>>({});
    const [lines, setLines] = useState<LineData[]>([]);

    const isNodeVisible = useCallback((key: string): boolean => {
        if (key === 'user' || key === 'gateway') return true;
        const node = NODES[key];
        if (node?.statusKeys) {
            return node.statusKeys.some(k => content[k] !== undefined);
        }
        return content[key] !== undefined;
    }, [content]);

    const getStatus = useCallback((key: string): 'up' | 'down' | 'unknown' => {
        if (key === 'user') return 'up';
        const node = NODES[key];
        // For merged nodes (e.g. broker = activemq/kafka), check if any key is UP
        if (node?.statusKeys) {
            const statuses = node.statusKeys.map(k => content[k]);
            if (statuses.every(s => !s)) return 'unknown';
            return statuses.some(s => s?.status === 'UP') ? 'up' : 'down';
        }
        const s = content[key];
        if (!s) return 'unknown';
        return s.status === 'UP' ? 'up' : 'down';
    }, [content]);

    useEffect(() => {
        const graph = graphRef.current;
        if (!graph) return;

        const updateLines = () => {
            const graphRect = graph.getBoundingClientRect();
            const newLines: LineData[] = [];

            for (const conn of CONNECTIONS) {
                const fromEl = nodeRefs.current[conn.from];
                const toEl = nodeRefs.current[conn.to];
                if (!fromEl || !toEl) continue;

                const fromRect = fromEl.getBoundingClientRect();
                const toRect = toEl.getBoundingClientRect();

                const fromStatus = getStatus(conn.from);
                const toStatus = getStatus(conn.to);
                const lineStatus = fromStatus === 'down' || toStatus === 'down' ? 'down'
                    : fromStatus === 'up' && toStatus === 'up' ? 'up' : 'unknown';

                newLines.push({
                    x1: fromRect.left + fromRect.width / 2 - graphRect.left,
                    y1: fromRect.top + fromRect.height / 2 - graphRect.top,
                    x2: toRect.left + toRect.width / 2 - graphRect.left,
                    y2: toRect.top + toRect.height / 2 - graphRect.top,
                    style: conn.style,
                    status: lineStatus,
                });
            }
            setLines(newLines);
        };

        // Wait for layout to settle
        requestAnimationFrame(updateLines);
        window.addEventListener('resize', updateLines);
        return () => window.removeEventListener('resize', updateLines);
    }, [content, getStatus]);

    const nodeClass = (type: string, status: string) => {
        const base = type === 'infra' ? 'ov-infra-node' : type === 'user' ? 'ov-user-node' : 'ov-service-node';
        return `${base} ov-node--${status}`;
    };

    const lineClass = (line: LineData) => {
        return `ov-line ov-line--${line.status} ov-line--${line.style}`;
    };

    return (
        <Container className="ov-container">
            <h2 className="ov-title">Architecture Overview</h2>
            <p className="ov-subtitle">
                Real-time dependency health — refreshes every 3s
                <span className="ov-legend">
                    <span className="ov-legend-item">
                        <svg width="24" height="8"><line x1="0" y1="4" x2="24" y2="4" stroke="#198754" strokeWidth="2"/></svg> HTTP
                    </span>
                    <span className="ov-legend-item">
                        <svg width="24" height="8"><line x1="0" y1="4" x2="24" y2="4" stroke="#6c757d" strokeWidth="2" strokeDasharray="4 3"/></svg> Async
                    </span>
                </span>
            </p>

            {error ? (
                <div className="ov-error">
                    Failed to fetch health status: {error.toString()}
                </div>
            ) : (
                <div className="ov-graph" ref={graphRef}>
                    <svg className="ov-svg">
                        {lines.map((line, i) => (
                            <line
                                key={i}
                                x1={line.x1} y1={line.y1}
                                x2={line.x2} y2={line.y2}
                                className={lineClass(line)}
                            />
                        ))}
                    </svg>

                    {Object.entries(NODES).filter(([key]) => isNodeVisible(key)).map(([key, node]) => {
                        const status = getStatus(key);
                        return (
                            <div
                                key={key}
                                ref={el => { nodeRefs.current[key] = el; }}
                                className={nodeClass(node.type, status)}
                                style={{ left: `${node.x}%`, top: `${node.y}%` }}
                                title={content[key]?.error || ''}
                            >
                                {node.type === 'user' && <BsPersonFill size={20} style={{ marginBottom: 2 }} />}
                                <div className="ov-node-label">{node.label}</div>
                                {node.type !== 'user' && (
                                    <Badge bg={status === 'up' ? 'success' : status === 'down' ? 'danger' : 'secondary'}>
                                        {status === 'up' ? 'UP' : status === 'down' ? 'DOWN' : '...'}
                                    </Badge>
                                )}
                            </div>
                        );
                    })}
                </div>
            )}

            <div className="ov-footer">
                <span className={`ov-loading ${isLoading ? '' : 'ov-loading--hidden'}`}>
                    <AiOutlineLoading />
                </span>
                Last refresh: {timestamp.toLocaleTimeString()}
            </div>
        </Container>
    );
};

export default Overview;
