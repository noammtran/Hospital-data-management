import menuData from '../../public/data/menu.json';

describe('Audit Log menu configuration', () => {
  it('links to audit-log and is visible only with VIEW_AUDIT_LOG', () => {
    const item = menuData.menu.find(menuItem => menuItem.route === 'audit-log');

    expect(item).toBeDefined();
    expect(item?.name).toBe('audit-log');
    expect(item?.type).toBe('link');
    expect(item?.permissions).toEqual({ only: 'VIEW_AUDIT_LOG' });
    expect(menuData.menu.some(menuItem => menuItem.route === 'patient-history')).toBe(false);
  });

  it('links Staff accounts to users and exposes it only with MANAGE_USERS', () => {
    const item = menuData.menu.find(menuItem => menuItem.route === 'users');

    expect(item).toBeDefined();
    expect(item?.name).toBe('users');
    expect(item?.type).toBe('link');
    expect(item?.permissions).toEqual({ only: 'MANAGE_USERS' });
  });
});
