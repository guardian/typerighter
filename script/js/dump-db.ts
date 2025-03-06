import { Pool } from 'pg';
import { createObjectCsvWriter } from 'csv-writer';
import { format } from 'date-fns';

const dbConfig = {
    user: 'tr-rule-manager-local',
    host: 'localhost',
    database: 'tr-rule-manager-local',
    password: 'tr-rule-manager-local',
    port: 5432,
};

const tableConfig: TableConfig = {
    tableName: 'rules_draft',
    batchSize: 1000,
    orderByColumn: 'id',
    columns: [
        'id',
        'rule_type',
        'pattern',
        'replacement',
        'category',
        'description',
        'ignore',
        'notes',
        'external_id',
        'force_red_rule',
        'advisory_rule',
        'created_by',
        'created_at',
        'updated_by',
        'updated_at',
        'revision_id',
        'is_archived',
        'rule_order',
    ],
    outputPath: './',
};

interface TableConfig {
    tableName: string;
    batchSize: number;
    orderByColumn: string;
    whereClause?: string;
    columns: string[];
    outputPath: string;
}

async function paginateAndExport(config: TableConfig) {
    const pool = new Pool(dbConfig);
    let offset = 0;
    let hasMoreRecords = true;
    let batchNumber = 1;

    // Create CSV writer
    const timestamp = format(new Date(), 'yyyyMMdd_HHmmss');
    const csvWriter = createObjectCsvWriter({
        path: `${config.outputPath}/export_${timestamp}_batch_${batchNumber}.csv`,
        header: config.columns.map(column => ({
            id: column,
            title: column
        }))
    });

    try {
        console.log('Starting export...');

        while (hasMoreRecords) {
            // Construct the SQL query
            const whereClause = config.whereClause ? `WHERE ${config.whereClause}` : '';
            const query = `
                SELECT ${config.columns.join(', ')}
                FROM ${config.tableName}
                ${whereClause}
                ORDER BY ${config.orderByColumn}
                LIMIT ${config.batchSize}
                OFFSET ${offset}
            `;

            // Execute query
            const result = await pool.query(query);
            const records = result.rows;

            if (records.length === 0) {
                hasMoreRecords = false;
                continue;
            }

            // Write batch to CSV
            await csvWriter.writeRecords(records);

            console.log(`Processed batch ${batchNumber}: ${records.length} records`);

            // Update for next iteration
            offset += config.batchSize;
            batchNumber++;
        }

        console.log('Export completed successfully!');
    } catch (error) {
        console.error('Error during export:', error);
        throw error;
    } finally {
        await pool.end();
    }
}

async function main() {
    try {
        await paginateAndExport(tableConfig);
    } catch (error) {
        console.error('Export failed:', error);
        process.exit(1);
    }
}

main();