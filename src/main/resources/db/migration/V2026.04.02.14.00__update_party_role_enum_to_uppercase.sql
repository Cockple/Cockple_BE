UPDATE member_party
SET role = 'PARTY_MANAGER' 
WHERE role = 'party_MANAGER';

UPDATE member_party 
SET role = 'PARTY_SUBMANAGER' 
WHERE role = 'party_SUBMANAGER';

UPDATE member_party 
SET role = 'PARTY_MEMBER' 
WHERE role = 'party_MEMBER';
