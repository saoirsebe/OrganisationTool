/**
 * @type {import('node-pg-migrate').ColumnDefinitions | undefined}
 */
export const shorthands = undefined;

/**
 * @param pgm {import('node-pg-migrate').MigrationBuilder}
 * @param run {() => void | undefined}
 * @returns {Promise<void> | void}
 */
export const up = (pgm) => {
    pgm.createTable('tasks', {
        id: 'id',
        user_id: { type: 'integer', notNull: true },
        description: { type: 'text', notNull: true },
        task_date: 'date',
        task_time: 'time',
        duration_minutes: 'integer',
        is_fixed: { type: 'boolean', notNull: true, default: false },
    });
};

/**
 * @param pgm {import('node-pg-migrate').MigrationBuilder}
 * @param run {() => void | undefined}
 * @returns {Promise<void> | void}
 */
export const down = (pgm) => {
    pgm.dropTable('tasks');
};
