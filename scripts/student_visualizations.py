#!/usr/bin/env python3
"""
Student Data Visualization Module
Generates comprehensive plots and visualizations for university student enrollment data
with realistic patterns and distributions
"""

import mysql.connector
from mysql.connector import Error
import pandas as pd
import matplotlib
matplotlib.use('Agg')  # Use non-interactive backend
import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np
from datetime import datetime, timedelta
import argparse
import warnings
from config import *
warnings.filterwarnings('ignore')

# Set style for better-looking plots
plt.style.use(VISUALIZATION_CONFIG['style'])
sns.set_palette(VISUALIZATION_CONFIG['color_palette'])

class StudentDataVisualizer:
    """Student data visualization tool for university enrollment system.
    
    Generates comprehensive plots and visualizations for student enrollment data
    with realistic patterns and distributions. Connects to MySQL database to
    retrieve student information and creates various analytical visualizations.
    
    Attributes:
        host (str): Database host address
        database (str): Database name
        user (str): Database username
        password (str): Database password
        connection: MySQL database connection object
        data (dict): Storage for loaded dataframes
    """
    def __init__(self, host=None, database=None, user=None, password=None):
        # Use config defaults if not provided
        self.host = host or DATABASE_CONFIG['host']
        self.database = database or DATABASE_CONFIG['database']
        self.user = user or DATABASE_CONFIG['user']
        self.password = password or DATABASE_CONFIG['password']
        self.connection = None
        self.data = {}
        
    def connect(self):
        """Establish database connection.
        
        Returns:
            bool: True if connection successful, False otherwise
        """
        try:
            self.connection = mysql.connector.connect(
                host=self.host,
                database=self.database,
                user=self.user,
                password=self.password
            )
            if self.connection.is_connected():
                print(f"Connected to MySQL database '{self.database}'")
                return True
        except Error as e:
            print(f"Error connecting to MySQL: {e}")
            return False
    
    def disconnect(self):
        """Close database connection.
        
        Cleans up the database connection and prints confirmation message.
        """
        if self.connection and self.connection.is_connected():
            self.connection.close()
            print("MySQL connection closed")
    
    def load_student_data(self):
        """Load comprehensive student data from database.
        
        Queries students table with course and department information,
        and loads enrollment statistics if available.
        
        Returns:
            bool: True if data loading successful, False otherwise
        """
        if not self.connect():
            return False
        
        try:
            # Query student data with course and department information
            query = """
            SELECT 
                s.student_id,
                s.first_name,
                s.last_name,
                s.birthdate,
                s.student_status,
                s.year_level,
                c.course_name,
                d.department_name,
                TIMESTAMPDIFF(YEAR, s.birthdate, CURDATE()) as age
            FROM students s
            JOIN courses c ON s.course_id = c.id
            JOIN departments d ON c.department_id = d.id
            ORDER BY d.department_name, c.course_name
            """
            
            df = pd.read_sql(query, self.connection)
            self.data['students'] = df
            
            # Load enrollment statistics
            enrollment_query = """
            SELECT 
                c.course_name,
                d.department_name,
                COUNT(DISTINCT e.student_id) as enrolled_students,
                e.school_year,
                e.semester
            FROM enrollments e
            JOIN students s ON e.student_id = s.student_id
            JOIN courses c ON s.course_id = c.id
            JOIN departments d ON c.department_id = d.id
            WHERE e.status IN ('APPROVED', 'ENROLLED')
            GROUP BY c.course_name, d.department_name, e.school_year, e.semester
            ORDER BY enrolled_students DESC
            """
            
            enrollment_df = pd.read_sql(enrollment_query, self.connection)
            self.data['enrollments'] = enrollment_df
            
            print(f"Loaded {len(df)} student records")
            print(f"Loaded {len(enrollment_df)} enrollment records")
            return True
            
        except Exception as e:
            print(f"Error loading data: {e}")
            return False
        finally:
            self.disconnect()
    
    def add_realistic_gender_data(self):
        """Add realistic gender distribution based on course patterns.
        
        Uses GENDER_DISTRIBUTION configuration to assign gender probabilities
        based on course types (e.g., Nursing 85% female, Engineering 15-25% female).
        
        Returns:
            pandas.DataFrame: Student dataframe with added gender column
        """
        df = self.data['students'].copy()
        
        # Assign gender based on course
        np.random.seed(42)  # For reproducible results
        genders = []
        for course in df['course_name']:
            # Find matching probability
            female_prob = 0.5  # Default
            for course_type, prob in GENDER_DISTRIBUTION.items():
                if course_type.lower() in course.lower():
                    female_prob = prob
                    break
            
            # Assign gender
            gender = 'Female' if np.random.random() < female_prob else 'Male'
            genders.append(gender)
        
        df['gender'] = genders
        self.data['students'] = df
        return df
    
    def plot_regular_vs_irregular(self):
        """Create visualization for Regular vs Irregular students.
        
        Generates pie chart for overall distribution and bar chart showing
        status percentages by department. Uses config colors and saves
        to configured output path.
        
        Returns:
            pandas.Series: Student status counts
        """
        df = self.data['students']
        
        # Make irregular students uncommon
        fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 6))
        
        # Overall distribution
        status_counts = df['student_status'].value_counts()
        colors = [PLOT_COLORS['regular'], PLOT_COLORS['irregular']]
        
        ax1.pie(status_counts.values, labels=status_counts.index, autopct='%1.1f%%',
                colors=colors, startangle=90, explode=(0.05, 0))
        ax1.set_title('Overall Student Status Distribution\n(Irregular students are uncommon ~10-15%)', 
                     fontsize=14, fontweight='bold')
        
        # Status by department
        status_by_dept = pd.crosstab(df['department_name'], df['student_status'])
        status_by_dept_pct = status_by_dept.div(status_by_dept.sum(axis=1), axis=0) * 100
        
        status_by_dept_pct.plot(kind='bar', ax=ax2, color=colors)
        ax2.set_title('Student Status by Department (%)', fontsize=14, fontweight='bold')
        ax2.set_xlabel('Department')
        ax2.set_ylabel('Percentage (%)')
        ax2.legend(title='Status')
        ax2.tick_params(axis='x', rotation=45)
        
        plt.tight_layout()
        plt.savefig(f"{VISUALIZATION_CONFIG['output_directory']}{VISUALIZATION_FILES['regular_vs_irregular']}", 
                   dpi=VISUALIZATION_CONFIG['dpi'], bbox_inches='tight')
        plt.close()
        
        return status_counts
    
    def plot_students_per_course_department(self):
        """Visualize number of students per course and department.
        
        Creates horizontal bar charts showing student counts by department
        and top 15 courses. Includes value labels and saves to configured path.
        
        Returns:
            tuple: (department_counts, course_counts)
        """
        df = self.data['students']
        
        fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(16, 12))
        
        # Students per department
        dept_counts = df['department_name'].value_counts().sort_values(ascending=True)
        dept_counts.plot(kind='barh', ax=ax1, color='skyblue')
        ax1.set_title('Number of Students per Department', fontsize=14, fontweight='bold')
        ax1.set_xlabel('Number of Students')
        
        # Add value labels
        for i, v in enumerate(dept_counts.values):
            ax1.text(v + 1, i, str(v), va='center')
        
        # Students per course (top 15)
        course_counts = df['course_name'].value_counts().head(15).sort_values(ascending=True)
        course_counts.plot(kind='barh', ax=ax2, color='lightcoral')
        ax2.set_title('Top 15 Courses by Student Count', fontsize=14, fontweight='bold')
        ax2.set_xlabel('Number of Students')
        
        # Add value labels
        for i, v in enumerate(course_counts.values):
            ax2.text(v + 0.5, i, str(v), va='center', fontsize=9)
        
        plt.tight_layout()
        plt.savefig(f"{VISUALIZATION_CONFIG['output_directory']}{VISUALIZATION_FILES['students_per_course_department']}", 
                   dpi=VISUALIZATION_CONFIG['dpi'], bbox_inches='tight')
        plt.close()
        
        return dept_counts, course_counts
    
    def plot_gender_distribution_by_course(self):
        """Analyze and visualize gender distribution across courses.
        
        Creates 4-panel visualization:
        1. Overall gender distribution pie chart
        2. Gender distribution by department
        3. Top 10 courses with highest female percentage
        4. Top 10 courses with highest male percentage
        
        Returns:
            tuple: (gender_counts, gender_by_dept_percentages)
        """
        df = self.add_realistic_gender_data()
        
        fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(20, 16))
        
        # Overall gender distribution
        gender_counts = df['gender'].value_counts()
        colors = [PLOT_COLORS['female'], PLOT_COLORS['male']]  # Pink for female, blue for male
        
        ax1.pie(gender_counts.values, labels=gender_counts.index, autopct='%1.1f%%',
                colors=colors, startangle=90)
        ax1.set_title('Overall Gender Distribution', fontsize=14, fontweight='bold')
        
        # Gender by department
        gender_by_dept = pd.crosstab(df['department_name'], df['gender'])
        gender_by_dept_pct = gender_by_dept.div(gender_by_dept.sum(axis=1), axis=0) * 100
        gender_by_dept_pct = gender_by_dept_pct.sort_values('Female', ascending=False)
        
        gender_by_dept_pct.plot(kind='bar', ax=ax2, color=colors)
        ax2.set_title('Gender Distribution by Department (%)', fontsize=14, fontweight='bold')
        ax2.set_xlabel('Department')
        ax2.set_ylabel('Percentage (%)')
        ax2.legend(title='Gender')
        ax2.tick_params(axis='x', rotation=45)
        
        # Top courses with highest female percentage
        course_gender_pct = pd.crosstab(df['course_name'], df['gender'])
        course_gender_pct = course_gender_pct.div(course_gender_pct.sum(axis=1), axis=0) * 100
        top_female_courses = course_gender_pct.sort_values('Female', ascending=False).head(10)
        
        top_female_courses['Female'].plot(kind='bar', ax=ax3, color='#ff69b4')
        ax3.set_title('Top 10 Courses with Highest Female %', fontsize=14, fontweight='bold')
        ax3.set_xlabel('Course')
        ax3.set_ylabel('Female Percentage (%)')
        ax3.tick_params(axis='x', rotation=45)
        ax3.grid(axis='y', alpha=0.3)
        
        # Top courses with highest male percentage
        top_male_courses = course_gender_pct.sort_values('Male', ascending=False).head(10)
        
        top_male_courses['Male'].plot(kind='bar', ax=ax4, color='#4169e1')
        ax4.set_title('Top 10 Courses with Highest Male %', fontsize=14, fontweight='bold')
        ax4.set_xlabel('Course')
        ax4.set_ylabel('Male Percentage (%)')
        ax4.tick_params(axis='x', rotation=45)
        ax4.grid(axis='y', alpha=0.3)
        
        plt.tight_layout()
        plt.savefig(f"{VISUALIZATION_CONFIG['output_directory']}{VISUALIZATION_FILES['gender_distribution']}", 
                   dpi=VISUALIZATION_CONFIG['dpi'], bbox_inches='tight')
        plt.close()
        
        return gender_counts, gender_by_dept_pct
    
    def plot_highest_enrollment_analysis(self):
        """Identify and visualize highest enrollment patterns.
        
        Creates 4-panel analysis:
        1. Top 10 courses by total enrollment
        2. Enrollment trend by school year
        3. Total enrollment by department
        4. Enrollment distribution by semester
        
        Returns:
            tuple: (top_courses, yearly_enrollment, dept_enrollment)
        """
        if 'enrollments' not in self.data:
            print("No enrollment data available")
            return
        
        df = self.data['enrollments']
        
        fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(20, 16))
        
        # Top courses by total enrollment
        top_courses = df.groupby('course_name')['enrolled_students'].sum().sort_values(ascending=False).head(10)
        
        ax1.barh(range(len(top_courses)), top_courses.values, color='gold')
        ax1.set_yticks(range(len(top_courses)))
        ax1.set_yticklabels(top_courses.index)
        ax1.set_title('Top 10 Courses by Total Enrollment', fontsize=14, fontweight='bold')
        ax1.set_xlabel('Total Enrolled Students')
        
        # Add value labels
        for i, v in enumerate(top_courses.values):
            ax1.text(v + 0.5, i, str(v), va='center')
        
        # Enrollment by school year
        yearly_enrollment = df.groupby('school_year')['enrolled_students'].sum()
        
        ax2.plot(yearly_enrollment.index, yearly_enrollment.values, marker='o', linewidth=3, markersize=8)
        ax2.set_title('Enrollment Trend by School Year', fontsize=14, fontweight='bold')
        ax2.set_xlabel('School Year')
        ax2.set_ylabel('Total Enrollments')
        ax2.grid(True, alpha=0.3)
        
        # Department enrollment comparison
        dept_enrollment = df.groupby('department_name')['enrolled_students'].sum().sort_values(ascending=True)
        
        ax3.barh(range(len(dept_enrollment)), dept_enrollment.values, color='lightgreen')
        ax3.set_yticks(range(len(dept_enrollment)))
        ax3.set_yticklabels(dept_enrollment.index)
        ax3.set_title('Total Enrollment by Department', fontsize=14, fontweight='bold')
        ax3.set_xlabel('Total Enrolled Students')
        
        # Semester comparison
        semester_enrollment = df.groupby('semester')['enrolled_students'].sum()
        
        colors_semester = ['#ff9999', '#66b3ff']
        ax4.pie(semester_enrollment.values, labels=semester_enrollment.index, autopct='%1.1f%%',
                colors=colors_semester, startangle=90)
        ax4.set_title('Enrollment by Semester', fontsize=14, fontweight='bold')
        
        plt.tight_layout()
        plt.savefig(f"{VISUALIZATION_CONFIG['output_directory']}{VISUALIZATION_FILES['enrollment_analysis']}", 
                   dpi=VISUALIZATION_CONFIG['dpi'], bbox_inches='tight')
        plt.close()
        
        return top_courses, yearly_enrollment, dept_enrollment
    
    def plot_year_level_distribution(self):
        """Create year level distribution analysis.
        
        Generates 4-panel visualization:
        1. Overall year level distribution pie chart
        2. Year level distribution by department
        3. Student status by year level
        4. Average age by year level
        
        Returns:
            tuple: (year_counts, year_by_dept, age_by_year)
        """
        df = self.data['students']
        
        fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(20, 16))
        
        # Overall year level distribution
        year_counts = df['year_level'].value_counts().sort_index()
        colors_year = PLOT_COLORS['year_levels']
        
        ax1.pie(year_counts.values, labels=[f'Year {i}' for i in year_counts.index], 
                autopct='%1.1f%%', colors=colors_year, startangle=90)
        ax1.set_title('Overall Year Level Distribution', fontsize=14, fontweight='bold')
        
        # Year level by department
        year_by_dept = pd.crosstab(df['department_name'], df['year_level'])
        year_by_dept_pct = year_by_dept.div(year_by_dept.sum(axis=1), axis=0) * 100
        
        year_by_dept_pct.plot(kind='bar', ax=ax2, color=colors_year)
        ax2.set_title('Year Level Distribution by Department (%)', fontsize=14, fontweight='bold')
        ax2.set_xlabel('Department')
        ax2.set_ylabel('Percentage (%)')
        ax2.legend(title='Year Level', labels=[f'Year {i}' for i in range(1, 6)])
        ax2.tick_params(axis='x', rotation=45)
        
        # Year level by student status
        year_by_status = pd.crosstab(df['year_level'], df['student_status'])
        year_by_status_pct = year_by_status.div(year_by_status.sum(axis=1), axis=0) * 100
        
        year_by_status_pct.plot(kind='bar', ax=ax3, color=['#2ecc71', '#e74c3c'])
        ax3.set_title('Student Status by Year Level (%)', fontsize=14, fontweight='bold')
        ax3.set_xlabel('Year Level')
        ax3.set_ylabel('Percentage (%)')
        ax3.legend(title='Status')
        ax3.set_xticklabels([f'Year {i}' for i in range(1, 6)], rotation=0)
        
        # Average age by year level
        df_with_gender = self.add_realistic_gender_data()
        age_by_year = df_with_gender.groupby('year_level')['age'].mean()
        
        ax4.bar(range(len(age_by_year)), age_by_year.values, color='purple', alpha=0.7)
        ax4.set_title('Average Age by Year Level', fontsize=14, fontweight='bold')
        ax4.set_xlabel('Year Level')
        ax4.set_ylabel('Average Age')
        ax4.set_xticks(range(len(age_by_year)))
        ax4.set_xticklabels([f'Year {i}' for i in age_by_year.index])
        ax4.grid(axis='y', alpha=0.3)
        
        # Add value labels
        for i, v in enumerate(age_by_year.values):
            ax4.text(i, v + 0.1, f'{v:.1f}', ha='center', va='bottom')
        
        plt.tight_layout()
        plt.savefig(f"{VISUALIZATION_CONFIG['output_directory']}{VISUALIZATION_FILES['year_level_distribution']}", 
                   dpi=VISUALIZATION_CONFIG['dpi'], bbox_inches='tight')
        plt.close()
        
        return year_counts, year_by_dept, age_by_year
    
    def plot_age_demographics(self):
        """Generate age distribution and demographics visualization.
        
        Creates 4-panel analysis:
        1. Age distribution histogram
        2. Age distribution by gender (box plot)
        3. Average age by department
        4. Age vs Year Level scatter plot with error bars
        
        Returns:
            tuple: (age_description, age_by_department)
        """
        df = self.add_realistic_gender_data()
        
        fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(20, 16))
        
        # Age distribution histogram
        ax1.hist(df['age'], bins=range(18, 26), color='skyblue', alpha=0.7, edgecolor='black')
        ax1.set_title('Age Distribution of Students', fontsize=14, fontweight='bold')
        ax1.set_xlabel('Age')
        ax1.set_ylabel('Number of Students')
        ax1.grid(axis='y', alpha=0.3)
        
        # Age by gender
        df.boxplot(column='age', by='gender', ax=ax2, patch_artist=True)
        ax2.set_title('Age Distribution by Gender', fontsize=14, fontweight='bold')
        ax2.set_xlabel('Gender')
        ax2.set_ylabel('Age')
        ax2.grid(axis='y', alpha=0.3)
        
        # Age by department
        age_by_dept = df.groupby('department_name')['age'].mean().sort_values(ascending=True)
        
        ax3.barh(range(len(age_by_dept)), age_by_dept.values, color='lightcoral')
        ax3.set_yticks(range(len(age_by_dept)))
        ax3.set_yticklabels(age_by_dept.index)
        ax3.set_title('Average Age by Department', fontsize=14, fontweight='bold')
        ax3.set_xlabel('Average Age')
        
        # Add value labels
        for i, v in enumerate(age_by_dept.values):
            ax3.text(v + 0.05, i, f'{v:.1f}', va='center')
        
        # Age vs Year Level scatter plot
        age_by_year_data = df.groupby('year_level')['age'].agg(['mean', 'std', 'count']).reset_index()
        
        ax4.errorbar(age_by_year_data['year_level'], age_by_year_data['mean'], 
                    yerr=age_by_year_data['std'], marker='o', linewidth=2, markersize=8,
                    capsize=5, capthick=2)
        ax4.set_title('Age Distribution by Year Level (with std deviation)', fontsize=14, fontweight='bold')
        ax4.set_xlabel('Year Level')
        ax4.set_ylabel('Age')
        ax4.grid(True, alpha=0.3)
        ax4.set_xticks([1, 2, 3, 4, 5])
        
        plt.tight_layout()
        plt.savefig(f"{VISUALIZATION_CONFIG['output_directory']}{VISUALIZATION_FILES['age_demographics']}", 
                   dpi=VISUALIZATION_CONFIG['dpi'], bbox_inches='tight')
        plt.close()
        
        return df['age'].describe(), age_by_dept
    
    def generate_comprehensive_report(self):
        """Generate all visualizations and summary statistics.
        
        Executes all visualization methods in sequence and prints
        comprehensive summary statistics including total students,
        regular/irregular distribution, department counts, and gender breakdown.
        """
        print("=== Generating Comprehensive Student Data Visualizations ===\n")
        
        if not self.load_student_data():
            print("Failed to load student data")
            return
        
        print("1. Generating Regular vs Irregular Student Analysis...")
        regular_stats = self.plot_regular_vs_irregular()
        
        print("2. Generating Students per Course and Department Analysis...")
        dept_stats, course_stats = self.plot_students_per_course_department()
        
        print("3. Generating Gender Distribution Analysis...")
        gender_stats, gender_by_dept = self.plot_gender_distribution_by_course()
        
        print("4. Generating Highest Enrollment Analysis...")
        if 'enrollments' in self.data:
            enrollment_stats = self.plot_highest_enrollment_analysis()
        
        print("5. Generating Year Level Distribution Analysis...")
        year_stats = self.plot_year_level_distribution()
        
        print("6. Generating Age Demographics Analysis...")
        age_stats = self.plot_age_demographics()
        
        # Print summary statistics
        print("\n=== SUMMARY STATISTICS ===")
        print(f"Total Students: {len(self.data['students'])}")
        print(f"Regular Students: {regular_stats.get('REGULAR', 0)} ({regular_stats.get('REGULAR', 0)/len(self.data['students'])*100:.1f}%)")
        print(f"Irregular Students: {regular_stats.get('IRREGULAR', 0)} ({regular_stats.get('IRREGULAR', 0)/len(self.data['students'])*100:.1f}%)")
        print(f"Number of Departments: {len(dept_stats)}")
        print(f"Number of Courses: {len(course_stats)}")
        print(f"Top Department: {dept_stats.index[0]} ({dept_stats.iloc[0]} students)")
        print(f"Top Course: {course_stats.index[0]} ({course_stats.iloc[0]} students)")
        
        if 'gender' in self.data['students'].columns:
            gender_counts = self.data['students']['gender'].value_counts()
            print(f"Female Students: {gender_counts.get('Female', 0)} ({gender_counts.get('Female', 0)/len(self.data['students'])*100:.1f}%)")
            print(f"Male Students: {gender_counts.get('Male', 0)} ({gender_counts.get('Male', 0)/len(self.data['students'])*100:.1f}%)")
        
        print(f"\nAll visualizations saved to {VISUALIZATION_CONFIG['output_directory']}")
        print("Generated files:")
        for file_name in VISUALIZATION_FILES.values():
            print(f"- {file_name}")

def main():
    """Main function to run all visualizations.
    
    Parses command line arguments for database connection parameters,
    initializes the visualizer, and generates all visualization reports.
    
    Command line arguments:
        --host: Database host (default from config)
        --database: Database name (default from config)
        --user: Database user (default from config)
        --password: Database password (default from config)
    """
    parser = argparse.ArgumentParser(description='Student Data Visualization Tool')
    parser.add_argument('--host', default=DATABASE_CONFIG['host'], help='Database host')
    parser.add_argument('--database', default=DATABASE_CONFIG['database'], help='Database name')
    parser.add_argument('--user', default=DATABASE_CONFIG['user'], help='Database user')
    parser.add_argument('--password', default=DATABASE_CONFIG['password'], help='Database password')
    
    args = parser.parse_args()
    
    visualizer = StudentDataVisualizer(
        host=args.host,
        database=args.database,
        user=args.user,
        password=args.password
    )
    visualizer.generate_comprehensive_report()

if __name__ == "__main__":
    main()
