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
    pgm.dropColumns('tasks', ['is_fixed']);
    pgm.addColumns('tasks', {
        deadline: { type: 'timestamptz' },
        priority: {
            type: 'smallint',
            notNull: true,
            default: 2,
            check: 'priority BETWEEN 1 AND 3',
        },
        date_is_fixed:{ type: 'boolean', notNull: true, default: false },
        time_is_fixed:{ type: 'boolean', notNull: true, default: false }
    });

};

/**
 * @param pgm {import('node-pg-migrate').MigrationBuilder}
 * @param run {() => void | undefined}
 * @returns {Promise<void> | void}
 */
export const down = (pgm) => {

    pgm.dropColumns('tasks', ['deadline', 'priority']);
    pgm.addColumns('tasks', {
        is_fixed: { type: 'boolean', notNull: true, default: false }
    });
};
