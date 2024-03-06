#!/bin/bash
echo "---
- name: 'Continuos Delivery to Fineract deployment'
  hosts: k8smanagerdev
  become: yes
  become_method: runas
  become_flags: logon_type=new_credentials logon_flags=netcredentials_only
  vars:
    ansible_become_user: 'ansible'
    ansible_become_pass: '4Ns1BL3-4DM1N**'
  tasks:
    - name: Copy fineract/backend-deployment.yaml file to set up the Fineract deployment
      copy:
        src: ./backend-deployment.yaml
        dest: /home/ansible

    - name: Apply the fineract/backend-deployment.yaml k8s manifest
      shell: 'kubectl apply -f backend-deployment.yaml'
      
" > deploy-fineract-backend-deployment-playbook.yaml